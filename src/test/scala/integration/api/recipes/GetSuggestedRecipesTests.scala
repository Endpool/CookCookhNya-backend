package integration.api.recipes

import api.Authentication.AuthenticatedUser
import api.recipes.SuggestedRecipesResp
import domain.{StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.decode
import zio.*
import zio.http.Request.get
import zio.http.{Response, Client, Path, Request, Status, URL}
import zio.test.*

object GetSuggestedRecipesTests extends ZIOIntegrationTestSpec:
  private val endpointPath = URL(Path.root / "recipes" / "suggested")

  private def getSuggestedRecipes(user: AuthenticatedUser, storageIds: Seq[StorageId]):
    RIO[Client, Response] =
    Client.batched(
      get(endpointPath)
        .addQueryParams("storage-id", Chunk.fromIterable(storageIds.map(_.toString)))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Get suggested recipes tests")(
    test("When unauthorized should get 401") {
      for
        resp <- Client.batched(
          Request.get(endpointPath)
        )
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized should get 200") {
      for
        user <- registerUser
        resp <- getSuggestedRecipes(user, Seq.empty)
      yield assertTrue(resp.status == Status.Ok)
    },
    test("When storages list is empty, for all recipes there should be 0 available ingredients") {
      for
        user <- registerUser

        n <- Gen.int(2, 10).runHead.some
        recipeIds <- ZIO.collectAll(
          (1 to n).map(i =>
            createNIngredients(i).flatMap(
              createRecipe(user, _)
            )
          )
        )

        resp <- getSuggestedRecipes(user, Seq.empty)

        bodyStr <- resp.body.asString
        suggestedRecipes <- ZIO.fromEither(decode[SuggestedRecipesResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(suggestedRecipes.recipes.map(_.id).forall(recipeIds.contains))
         && assertTrue(suggestedRecipes.recipes.map(_.available).forall(_ == 0))
    },
  ).provideLayer(testLayer)

