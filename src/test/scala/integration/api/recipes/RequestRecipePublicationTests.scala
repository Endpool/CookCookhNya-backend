package integration.api.recipes

import api.Authentication.AuthenticatedUser
import domain.RecipeId
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.decode
import zio.*
import zio.http.*
import zio.test.*

object RequestRecipePublicationTests extends ZIOIntegrationTestSpec:
  private def endpointPath(recipeId: RecipeId): URL =
    URL(Path.root / "recipes" / recipeId.toString / "request-publication")

  private def requestRecipePublication(user: AuthenticatedUser, recipeId: RecipeId):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath(recipeId))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Request recipe publication tests")(
    test("When unauthorized should get 401") {
      for
        recipeId <- getRandomUUID
        resp <- Client.batched(
          post(endpointPath(recipeId))
        )
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized should get 201") {
      for
        user <- registerUser

        recipeId <- createCustomRecipe(user, Vector.empty)

        resp <- requestRecipePublication(user, recipeId)
      yield assertTrue(resp.status == Status.Created)
    },
  ).provideLayer(testLayer)

