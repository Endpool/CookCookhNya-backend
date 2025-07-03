package integration.api.storages

import api.storages.StorageSummaryResp
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.StorageId
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.decode
import zio.http.Request.get
import zio.http.{Client, Status, URL, Path}
import zio.test.Assertion.*
import zio.test.{Gen, TestEnvironment, assertTrue, Spec}
import zio.{Scope, ZIO}

object GetStorageTests extends ZIOIntegrationTestSpec:
  private def endpointPath(storageId: StorageId): URL =
    URL(Path.root / "my" / "storages" / storageId.toString)

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Get storage tests")(
    test("When unauthorized should get 401") {
      for
        storageId <- Gen.long(1, 10000000).runHead.some
        resp <- Client.batched(get(endpointPath(storageId)))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized but storage does not exist should get 404") {
      for
        userId <- registerUser
        storageId <- Gen.long(1, 10000000).runHead.some

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(userId)
        )
      yield assertTrue(resp.status == Status.NotFound)
    },
    test("When authorized and user owns the storage should get 200 and the storage") {
      for
        userId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, userId) }

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(userId)
        )

        bodyStr <- resp.body.asString
        storage <- ZIO.fromEither(decode[StorageSummaryResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storage.id == storageId)
         && assertTrue(storage.name == storageName)
         && assertTrue(storage.ownerId == userId)
    },
    test("When authorized and user is a member of the storage should get 200 and the storage") {
      for
        creatorId <- registerUser
        userId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, creatorId) }
        _ <- ZIO.serviceWithZIO[StorageMembersRepo] { _.addMemberToStorageById(storageId, userId) }

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(userId)
        )

        bodyStr <- resp.body.asString
        storage <- ZIO.fromEither(decode[StorageSummaryResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storage.id == storageId)
         && assertTrue(storage.name == storageName)
         && assertTrue(storage.ownerId == creatorId)
    },
    test("When authorized but user is neither the owner nor a member should get 404") {
      for
        creatorId <- registerUser
        n <- Gen.int(0, 10).runHead.some
        memberIds <- registerNUsers(n)
        userId <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo] { _.createEmpty(storageName, creatorId) }
        _ <- ZIO.serviceWithZIO[StorageMembersRepo] { repo =>
          ZIO.foreach(memberIds) { repo.addMemberToStorageById(storageId, _) }
        }

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(userId)
        )
      yield assertTrue(resp.status == Status.NotFound)
    }
  ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] = for
    base <- Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf")
    suffix <- Gen.elements("", "-1", "-2", "-Main", "-Backup")
  yield base + suffix
