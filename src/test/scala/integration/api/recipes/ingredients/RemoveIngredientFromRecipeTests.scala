package integration.api.recipes.ingredients

import api.Authentication.AuthenticatedUser
import api.recipes.ingredients.CannotModifyPublishedRecipe
import db.repositories.{IngredientsRepo, RecipesRepo}
import db.repositories.RecipeIngredientsRepo
import domain.{RecipeId, RecipeNotFound, IngredientId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.*
import zio.http.*
import zio.http.Request.delete
import zio.*
import zio.test.*

object RemoveIngredientFromRecipeTests extends ZIOIntegrationTestSpec:
  private def endpointPath(recipeId: RecipeId, ingredientId: IngredientId): URL =
    URL(Path.root / "recipes" / recipeId.toString / "ingredients" / ingredientId.toString)

  private def removeIngredientFromRecipe(
    user: AuthenticatedUser,
    recipeId: RecipeId,
    ingredientId: IngredientId,
  ): RIO[Client, Response] =
    Client.batched(
      delete(endpointPath(recipeId, ingredientId))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Remove ingredient from recipe tests")(
    test("When unauthorized should get 401") {
      for
        recipeId <- getRandomUUID
        ingredientId <- getRandomUUID

        resp <- Client.batched(
          delete(endpointPath(recipeId, ingredientId))
        )
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized should get 204") {
      for
        user <- registerUser

        ingredientId <- createPublicIngredient
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, otherIngredientIds :+ ingredientId)

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)
      yield assertTrue(resp.status == Status.NoContent)
    },
    test("""When removing public ingredient from created unpublished recipe,
            ingredient should be removed from the recipe""") {
      for
        user <- registerUser

        ingredientId <- createPublicIngredient
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, otherIngredientIds :+ ingredientId)

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(!recipeIngredients.contains(ingredientId))
    },
    test("""When removing custom ingredient from created unpublished recipe,
            ingredient should be removed from the recipe""") {
      for
        user <- registerUser

        ingredientId <- createCustomIngredient(user)
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, otherIngredientIds :+ ingredientId)

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(!recipeIngredients.contains(ingredientId))
    },
    test("""When removing ingredient from other user's custom unpublished recipe,
            should get 404 recipe not found and ingredient should NOT be removed from the recipe""") {
      for
        ingredientId <- createPublicIngredient
        otherUser <- registerUser
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(otherUser, otherIngredientIds :+ ingredientId)

        user <- registerUser

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeNotFound <- resp.body.asString.map(decode[RecipeNotFound])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(recipeNotFound.is(_.right).recipeId == recipeId)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("""When removing other user's ingredient from other user's custom unpublished recipe,
            should get 404 recipe not found and ingredient should NOT be removed from the recipe""") {
      for
        otherUser <- registerUser
        ingredientId <- createCustomIngredient(otherUser)
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(otherUser, otherIngredientIds :+ ingredientId)

        user <- registerUser

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeNotFound <- resp.body.asString.map(decode[RecipeNotFound])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(recipeNotFound.is(_.right).recipeId == recipeId)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("""When removing public ingredient from published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be removed from the recipe""") {
      for
        ingredientId <- createPublicIngredient

        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- registerUser.flatMap(createCustomRecipe(_, otherIngredientIds :+ ingredientId))
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        user <- registerUser

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("""When removing public ingredient from own created published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be removed from the recipe""") {
      for
        ingredientId <- createPublicIngredient
        user <- registerUser
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, otherIngredientIds :+ ingredientId)
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("""When removing custom ingredient from own created published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be removed from the recipe""") {
      for
        user <- registerUser
        ingredientId <- createCustomIngredient(user)
        otherIngredientIds <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, otherIngredientIds :+ ingredientId)
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("""When removing non-existant ingredient from created unpublished recipe without that ingredient,
            should get 204 and recipe should not change""") {
      for
        user <- registerUser
        initialRecipeIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, initialRecipeIngredients)

        ingredientId <- getRandomUUID

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialRecipeIngredients)
    },
    test("""When removing other user's custom ingredient from created unpublished recipe without that ingredient,
            should get 204 and recipe should not change""") {
      for
        otherUser <- registerUser
        ingredientId <- createCustomIngredient(otherUser)

        user <- registerUser
        initialRecipeIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, initialRecipeIngredients)

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialRecipeIngredients)
    },
    test("""When removing public ingredient from created unpublished recipe without that ingredient,
            should get 204 and recipe should not change""") {
      for
        user <- registerUser
        initialRecipeIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, initialRecipeIngredients)

        ingredientId <- createPublicIngredient

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialRecipeIngredients)
    },
    test("""When removing custom ingredient from created unpublished recipe without that ingredient,
            should get 204 and recipe should not change""") {
      for
        user <- registerUser
        initialRecipeIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNIngredients)
        recipeId <- createCustomRecipe(user, initialRecipeIngredients)

        ingredientId <- createCustomIngredient(user)

        resp <- removeIngredientFromRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialRecipeIngredients)
    },
  ).provideLayer(testLayer)
