package integration.api.ingredients

import api.storages.CreateStorageReqBody
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.Scope
import zio.test.{Spec, TestEnvironment, assertTrue}

object CreateIngredientTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Create ingredient tests")(
      test("Should result in 200") {
        for
          resp <- Client.batched(
            post("ingredients")
              .withJsonBody(CreateStorageReqBody("ingredient"))
          )
        yield assertTrue(resp.status == Status.Ok)
      },
    ).provideLayer(testLayer)
