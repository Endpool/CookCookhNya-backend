package integration.api.recipes.ingredients

import api.Authentication.AuthenticatedUser
import db.repositories.{IngredientsRepo, RecipesRepo}
import domain.{IngredientId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import zio.http.{Client, Path, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.http.Response
import zio.test.*
import domain.RecipeId
import db.repositories.RecipeIngredientsRepo

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

        recipeId <- createRecipe(user, Vector.empty)
        ingredientId <- createIngredient

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)
      yield assertTrue(resp.status == Status.NoContent)
    },
    test("When adding public ingredient to created unpublished recipe, ingredient should be added to the recipe") {
      for
        user <- registerUser

        recipeId <- createRecipe(user, Vector.empty)
        ingredientId <- createIngredient

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
    test("When adding custom ingredient to created unpublished recipe, ingredient should be added to the recipe") {
      for
        user <- registerUser

        recipeId <- createRecipe(user, Vector.empty)
        ingredientId <- createCustomIngredient(user)

        resp <- addIngredientToRecipe(user, recipeId, ingredientId)

        recipeIngredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
          .getAllIngredients(recipeId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(recipeIngredients.contains(ingredientId))
    },
  ).provideLayer(testLayer)
