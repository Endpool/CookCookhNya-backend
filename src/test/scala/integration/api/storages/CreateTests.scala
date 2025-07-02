package integration.api.storages

import api.storages.CreateStorageReqBody
import db.dbLayer
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.Scope
import zio.test.Assertion.*
import zio.test.{TestEnvironment, assertTrue, Spec}

object CreateStorageTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Create storage tests")(
      test("When unauthorization should get 401") {
        Client.batched(
          post("my/storages")
            .withJsonBody(CreateStorageReqBody("storage"))
        ).map(resp => assertTrue(resp.status == Status.Unauthorized))
      },
    ).provideLayer(testLayer)
