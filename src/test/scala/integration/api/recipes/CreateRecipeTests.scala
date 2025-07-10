package integration.api.recipes

import api.recipes.{IngredientSummary, RecipeResp}
import api.Authentication.AuthenticatedUser
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo}
import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Request, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.test.{Spec, TestEnvironment, assertTrue}
import zio.http.Response
import api.recipes.CreateRecipeReqBody

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
  ).provideLayer(testLayer)

