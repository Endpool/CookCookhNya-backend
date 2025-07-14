package integration.api.recipes.ingredients

import api.Authentication.AuthenticatedUser
import api.recipes.CreateRecipeReqBody
import db.repositories.{IngredientsRepo, RecipesRepo}
import db.tables.recipesTable
import domain.{IngredientId, IngredientNotFound}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import com.augustnagro.magnum.magzio.{Transactor, sql}
import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.http.Response
import zio.test.*
import zio.Cause
import domain.RecipeId

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
  ).provideLayer(testLayer)
