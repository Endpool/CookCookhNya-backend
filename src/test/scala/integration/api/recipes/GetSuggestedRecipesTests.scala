package integration.api.recipes

import api.Authentication.AuthenticatedUser
import domain.{StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

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

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Get suggested recipes tests")(
      test("When unauthorized should get 401") {
        for
          resp <- Client.batched(
            Request.get(endpointPath)
          )
        yield (assertTrue(resp.status == Status.Unauthorized))
      },
    ).provideLayer(testLayer)

