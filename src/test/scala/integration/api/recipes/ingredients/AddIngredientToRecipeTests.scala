package integration.api.recipes.ingredients

import api.Authentication.AuthenticatedUser
import api.recipes.ingredients.CannotModifyPublishedRecipe
import db.repositories.{IngredientsRepo, RecipesRepo}
import db.repositories.RecipeIngredientsRepo
import domain.{RecipeId, RecipeNotFound, IngredientId, IngredientNotFound}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.*
import zio.http.*
import zio.*
import zio.test.*

object AddIngredientToRecipeTests extends ZIOIntegrationTestSpec:
  private def endpointPath(recipeId: RecipeId, ingredientId: IngredientId): URL =
    URL(Path.root / "recipes" / recipeId.toString / "ingredients" / ingredientId.toString)

  private def addIngredientToRecipe(
    user: AuthenticatedUser,
    recipeId: RecipeId,
    ingredientId: IngredientId,
  ): RIO[Client, Response] =
    Client.batched(
      put(endpointPath(recipeId, ingredientId))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Add ingredient to recipe tests")(
    test("When unauthorized should get 401") {
      for
        recipeId <- getRandomUUID
        ingredientId <- getRandomUUID

        resp <- Client.batched(
          put(endpointPath(recipeId, ingredientId))
        )
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized should get 204") {
      for
        user <- registerUser

        recipeId <- createNPublicIngredients(5)
          .flatMap(createCustomRecipe(user, _))
        ingredientId <- createPublicIngredient

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)
      yield assertTrue(resp.status == Status.NoContent)
    },
    test("""When adding public ingredient to created unpublished recipe,
            ingredient should be added to the recipe""") {
      for
        user <- registerUser

        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(user, initialIngredients)
        ingredientId <- createPublicIngredient

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients contains ingredientId)
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding custom ingredient to created unpublished recipe,
            ingredient should be added to the recipe""") {
      for
        user <- registerUser

        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(user, initialIngredients)
        ingredientId <- createCustomIngredient(user)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients contains ingredientId)
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding other user's custom ingredient to created unpublished recipe,
            should get 404 ingredient not found and ingredient should NOT be added to the recipe""") {
      for
        otherUser <- registerUser
        ingredientId <- createCustomIngredient(otherUser)

        user <- registerUser
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(user, initialIngredients)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        ingredientNotFound <- resp.body.asString.map(decode[IngredientNotFound])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(ingredientNotFound.is(_.right).ingredientId == ingredientId.toString)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding public ingredient to other user's custom unpublished recipe,
            should get 404 recipe not found and ingredient should NOT be added to the recipe""") {
      for
        otherUser <- registerUser
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(otherUser, initialIngredients)

        ingredientId <- createPublicIngredient

        user <- registerUser

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeNotFound <- resp.body.asString.map(decode[RecipeNotFound])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(recipeNotFound.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding custom ingredient to other user's custom unpublished recipe,
            should get 404 recipe not found and ingredient should NOT be added to the recipe""") {
      for
        otherUser <- registerUser
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(otherUser, initialIngredients)

        user <- registerUser
        ingredientId <- createCustomIngredient(user)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeNotFound <- resp.body.asString.map(decode[RecipeNotFound])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(recipeNotFound.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding public ingredient to published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be added to the recipe""") {
      for
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- registerUser.flatMap(createCustomRecipe(_,  initialIngredients))
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        ingredientId <- createPublicIngredient

        user <- registerUser

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding custom ingredient to published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be added to the recipe""") {
      for
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- registerUser.flatMap(createCustomRecipe(_,  initialIngredients))
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        user <- registerUser
        ingredientId <- createCustomIngredient(user)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding public ingredient to own created published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be added to the recipe""") {
      for
        user <- registerUser
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(user, initialIngredients)
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        ingredientId <- createPublicIngredient

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding custom ingredient to own created published recipe,
            should get 403 cannot modify published recipe and ingredient should NOT be added to the recipe""") {
      for
        user <- registerUser
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
        recipeId <- createCustomRecipe(user, initialIngredients)
        _ <- ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))

        ingredientId <- createCustomIngredient(user)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        error <- resp.body.asString.map(decode[CannotModifyPublishedRecipe])
        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(error.is(_.right).recipeId == recipeId)
         && assertTrue(!recipeIngredients.contains(ingredientId))
         && assertTrue(initialIngredients isSubsetOf recipeIngredients)
    },
    test("""When adding public ingredient to created unpublished recipe which already has that ingredient,
            should get 204 and recipe should not change""") {
      for
        user <- registerUser

        ingredientId <- createPublicIngredient
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
          .map(_ :+ ingredientId)
        recipeId <- createCustomRecipe(user, initialIngredients)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialIngredients)
    },
    test("""When adding custom ingredient to created unpublished recipe which already has that ingredient,
            should get 204 and recipe should not change""") {
      for
        user <- registerUser

        ingredientId <- createCustomIngredient(user)
        initialIngredients <- Gen.int(0, 5).runHead.some
          .flatMap(createNPublicIngredients)
          .map(_ :+ ingredientId)
        recipeId <- createCustomRecipe(user, initialIngredients)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients hasSameElementsAs initialIngredients)
    },
  ).provideLayer(testLayer)
