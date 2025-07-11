package integration.api.recipes

import api.Authentication.AuthenticatedUser
import api.recipes.CreateRecipeReqBody
import api.recipes.{RecipeResp}
import db.repositories.{IngredientsRepo, RecipesRepo}
import domain.{IngredientId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Request, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.http.Response
import zio.test.{
  Gen,
  TestEnvironment,
  assertTrue,
  Spec,
  SmartAssertionOps, TestLensOptionOps
}

object CreateRecipeTests extends ZIOIntegrationTestSpec:
  private val endpointPath = URL(Path.root / "recipes")

  private def createRecipe(user: AuthenticatedUser, reqBody: CreateRecipeReqBody):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath)
        .withJsonBody(reqBody)
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Create recipe tests")(
    test("When unauthorized should get 401") {
      Client.batched(
        post(endpointPath)
          .withJsonBody(CreateRecipeReqBody("recipe", "sourceLink", Vector.empty))
      ).map(resp => assertTrue(resp.status == Status.Unauthorized))
    },
    test("When authorized should get 200") {
      for
        user <- registerUser
        resp <- createRecipe(user, CreateRecipeReqBody("recipe", "sourceLink", Vector.empty))
      yield assertTrue(resp.status == Status.Ok)
    },
    test("When create valid recipe, recipe should be added to db") {
      for
        user <- registerUser
        recipeName <- randomString
        recipeSourceLink <- randomString
        ingredientIds <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
          Gen.string.runCollectN(10).flatMap(ZIO.foreach(_)(repo.add))
        ).map(_.map(_.id))
          .map(Vector.from)

        resp <- createRecipe(user, CreateRecipeReqBody(recipeName, recipeSourceLink, ingredientIds))
        recipeId <- resp.body.asString.map(_.replaceAll("\"", "").toUUID)
        recipe <- ZIO.serviceWithZIO[RecipesRepo](_.getRecipe(recipeId))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(recipe.is(_.some).name == recipeName)
         && assertTrue(recipe.is(_.some).sourceLink == recipeSourceLink)
         && assertTrue(recipe.get.ingredients hasSameElementsAs ingredientIds)
    },
  ).provideLayer(testLayer)

