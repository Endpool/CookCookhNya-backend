package integration.api.storages

import api.storages.CreateStorageReqBody
import db.repositories.StoragesRepo
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.{Scope, ZIO}
import zio.test.{
  TestEnvironment,
  assertTrue,
  Spec,
  SmartAssertionOps, TestLensOptionOps
}

object CreateStorageTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Create storage tests")(
      test("When unauthorized should get 401") {
        Client.batched(
          post("my/storages")
            .withJsonBody(CreateStorageReqBody("storage"))
        ).map(resp => assertTrue(resp.status == Status.Unauthorized))
      },
      test("When authorized should get 200") {
        for
          userId <- registerUser
          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )
        yield assertTrue(resp.status == Status.Ok)
      },
      test("When authorized, storage should be added to db & have its creator an owner") {
        val storageName = "storage"
        for
          userId <- registerUser

          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(CreateStorageReqBody(storageName))
              .addAuthorization(userId)
          )

          storageId <- resp.body.asString.map(_.toIntOption).someOrFailException
          storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
        yield assertTrue(resp.status == Status.Ok) &&
          assertTrue(storage.is(_.some).id == storageId) &&
          assertTrue(storage.is(_.some).name == storageName) &&
          assertTrue(storage.is(_.some).ownerId == userId)
      },
    ).provideLayer(testLayer)
