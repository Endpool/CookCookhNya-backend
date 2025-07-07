package integration.api.storages

import db.repositories.{StoragesRepo, StorageMembersRepo}
import domain.StorageId
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import zio.http.{Client, Status, URL, Path}
import zio.http.Request.delete
import zio.{Scope, ZIO, ZLayer}
import zio.test.{Gen, TestEnvironment, assertTrue, Spec}

object DeleteStorageTests extends ZIOIntegrationTestSpec:
  private def endpointPath(storageId: StorageId): URL =
    URL(Path.root / "my" / "storages" / storageId.toString)

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Delete storage tests")(
    test("When unauthorized should get 401") {
      for
        storageId <- Gen.long(1, 10000000).runHead.some
        resp <- Client.batched(delete(endpointPath(storageId)))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized and storage does not exist should get 204") {
      for
        userId <- registerUser
        storageId <- Gen.long(1, 10000000).runHead.some

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(userId)
        )
      yield assertTrue(resp.status == Status.NoContent)
    },
    test("When user owns the storage should get 204 and storage should be deleted") {
      for
        user <- registerUser
        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(user))
        )

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(user)
        )

        storageDoesNotExist <- ZIO.serviceWithZIO[StoragesRepo](_
          .getById(storageId)
          .provideLayer(ZLayer.succeed(user))
          .map(_.isEmpty)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(storageDoesNotExist)
    },
    test("When user is a member should get 403 and storage should not be deleted") {
      for
        creator <- registerUser
        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(creator))
        )
        user <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(storageId, user.userId))

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(user)
        )

        storageExists <- ZIO.serviceWithZIO[StoragesRepo](_
          .getById(storageId)
          .provideLayer(ZLayer.succeed(user))
          .map(_.isDefined)
        )
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(storageExists)
    },
    test("When user is neither the owner not a member should get 204 and storage should not be deleted") {
      for
        creator <- registerUser
        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(creator))
        )
        user <- registerUser

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(user)
        )

        storageExists <- ZIO.serviceWithZIO[StoragesRepo](_
          .getById(storageId)
          .provideUser(user)
          .map(_.isDefined)
        )
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(storageExists)
    }
  ).provideLayer(testLayer)
