package integration.api.storages

import api.storages.CreateStorageReqBody
import db.dbLayer
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.{Scope, ZIO}
import zio.test.Assertion.*
import zio.test.{TestEnvironment, assertTrue, Spec, SmartAssertionOps, SmartAssertMacros}
import zio.test.*
import db.repositories.StoragesRepo

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
          _ <- authorize
          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
          )
        yield assertTrue(resp.status == Status.Ok)
      },
      test("When authorized storage should be added to db") {
        var storageName = "storage"
        for
          _ <- authorize

          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
          )

          storageId <- resp.body.asString.map(_.toIntOption).someOrFailException
          storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
        yield assertTrue(resp.status == Status.Ok)
           && assert(storage)(isSome)
           && assertTrue(storage.is(_.some).id == storageId)
           && assertTrue(storage.is(_.some).name == storageName)
      },
      test("When created storage should have creator as owner") {
        var storageName = "storage"
        for
          userId <- authorize

          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
          )

          storageId <- resp.body.asString.map(_.toIntOption).someOrFailException
          storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storage.is(_.some).ownerId == userId)
      },
    ).provideLayer(testLayer)
