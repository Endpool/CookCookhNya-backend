package integration.api.storages

import db.repositories.{StoragesRepo, StorageMembersRepo}
import domain.StorageId
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import zio.http.{Client, Status, URL, Path}
import zio.http.Request.delete
import zio.{Scope, ZIO}
import zio.test.Assertion.*
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
        userId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, userId) }

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(userId)
        )

        storageDoesNotExist <-
          ZIO.serviceWithZIO[StoragesRepo] { _.getById(storageId).map(_.isEmpty) }
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(storageDoesNotExist)
    },
    test("When user is a member should get 403 and storage should not be deleted") {
      for
        creatorId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, creatorId) }
        userId <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo] { _.addMemberToStorageById(storageId, userId) }

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(userId)
        )

        storageExists <- ZIO.serviceWithZIO[StoragesRepo] { _.getById(storageId).map(_.isDefined) }
      yield assertTrue(resp.status == Status.Forbidden)
         && assertTrue(storageExists)
    },
    test("When user is neither the owner not a member should get 204 and storage should not be deleted") {
      for
        creatorId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, creatorId) }
        userId <- registerUser

        resp <- Client.batched(
          delete(endpointPath(storageId))
            .addAuthorization(userId)
        )

        storageExists <- ZIO.serviceWithZIO[StoragesRepo] { _.getById(storageId).map(_.isDefined) }
      yield assertTrue(resp.status == Status.NoContent)
         && assertTrue(storageExists)
    }
  ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] = for
    base <- Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf")
    suffix <- Gen.elements("", "-1", "-2", "-Main", "-Backup")
  yield base + suffix
