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
import zio.test.{Gen, TestEnvironment, assertTrue, Spec}
import zio.{Scope, ZIO, ZLayer}

object GetStorageSummaryTests extends ZIOIntegrationTestSpec:
  private def endpointPath(storageId: StorageId): URL =
    URL(Path.root / "storages" / storageId.toString)

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Get storage tests")(
    test("When unauthorized should get 401") {
      for
        storageId <- getRandomUUID
        resp <- Client.batched(get(endpointPath(storageId)))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized but storage does not exist should get 404") {
      for
        userId <- registerUser
        storageId <- getRandomUUID

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(userId)
        )
      yield assertTrue(resp.status == Status.NotFound)
    },
    test("When authorized and user owns the storage should get 200 and the storage") {
      for
        user <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(user))
        )

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(user)
        )

        bodyStr <- resp.body.asString
        storage <- ZIO.fromEither(decode[StorageSummaryResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storage.id == storageId)
         && assertTrue(storage.name == storageName)
         && assertTrue(storage.ownerId == user.userId)
    },
    test("When authorized and user is a member of the storage should get 200 and the storage") {
      for
        creator <- registerUser
        user <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(creator))
        )
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(storageId, user.userId))

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(user)
        )

        bodyStr <- resp.body.asString
        storage <- ZIO.fromEither(decode[StorageSummaryResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storage.id == storageId)
         && assertTrue(storage.name == storageName)
         && assertTrue(storage.ownerId == creator.userId)
    },
    test("When authorized but user is neither the owner nor a member should get 404") {
      for
        creator <- registerUser
        n <- Gen.int(0, 10).runHead.some
        members <- registerNUsers(n)
        user <- registerUser
        storageName <- storageNameGen.sample.map(_.value).runHead.some
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideLayer(ZLayer.succeed(creator))
        )
        _ <- ZIO.serviceWithZIO[StorageMembersRepo] { repo =>
          ZIO.foreach(members)(mem => repo.addMemberToStorageById(storageId, mem.userId))
        }

        resp <- Client.batched(
          get(endpointPath(storageId))
            .addAuthorization(user)
        )
      yield assertTrue(resp.status == Status.NotFound)
    }
  ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] = for
    base <- Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf")
    suffix <- Gen.elements("", "-1", "-2", "-Main", "-Backup")
  yield base + suffix
