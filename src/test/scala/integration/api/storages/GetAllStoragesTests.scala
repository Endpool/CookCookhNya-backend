package integration.api.storages

import api.storages.CreateStorageReqBody
import db.dbLayer
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.http.Request.get
import zio.{Scope, ZIO}
import zio.test.Assertion.*
import zio.test.{TestEnvironment, assertTrue, Spec, SmartAssertionOps, SmartAssertMacros}
import db.repositories.StoragesRepo
import zio.test.Gen
import io.circe.parser.decode
import api.storages.StorageSummaryResp
import db.repositories.StorageMembersRepo

object GetAllStoragesTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Get all storages tests")(
      test("When unauthorized should get 401") {
        Client.batched(
          get("my/storages")
        ).map(resp => assertTrue(resp.status == Status.Unauthorized))
      },
      test("When authorized and no storages should get 200 and empty list") {
        for
          userId <- registerUser
          resp <- Client.batched(
            get("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )
          bodyStr <- resp.body.asString
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(bodyStr == "[]")
      },
      test("When authorized and there are owned storages should get 200 and all owned storages") {
        for
          userId <- registerUser
          n <- Gen.int(1, 10).runHead.map(_.getOrElse(4))
          storageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(storageNames){ name => repo.createEmpty(name, userId) }
          }

          resp <- Client.batched(
            get("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
          storages <- ZIO.fromEither(decode[Vector[StorageSummaryResp]](bodyStr))
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storages.length == storageNames.length)
           && assertTrue(storages.forall(_.ownerId == userId))
           && assertTrue(storages.map(_.name).forall(storageNames.contains))
           && assertTrue(storageNames.forall(storages.map(_.name).contains))
      },
      test("When authorized and there are membered storages should get 200 and all membered storages") {
        for
          creatorId <- registerUser
          userId <- registerUser
          n <- Gen.int(1, 10).runHead.map(_.getOrElse(4))
          storageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          storageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(storageNames){ name => repo.createEmpty(name, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(storageIds){ id => repo.addMemberToStorageById(id, userId) }
          }

          resp <- Client.batched(
            get("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
          storages <- ZIO.fromEither(decode[Vector[StorageSummaryResp]](bodyStr))
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storages.length == storageNames.length)
           && assertTrue(storages.map(_.name).forall(storageNames.contains))
           && assertTrue(storageNames.forall(storages.map(_.name).contains))
      },
      test("When there are owned and membered storages should get 200 and all owned and membered storages") {
        for
          n <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          ownedStorageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          m <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          memberedStorageNames <- storageNameGen.sample.map(_.value).take(m).runCollect
          storageNames = ownedStorageNames ++ memberedStorageNames

          userId <- registerUser
          _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(ownedStorageNames){ name => repo.createEmpty(name, userId) }
          }

          creatorId <- registerUser
          memberedStorageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(memberedStorageNames){ name => repo.createEmpty(name, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(memberedStorageIds){ id => repo.addMemberToStorageById(id, userId) }
          }

          resp <- Client.batched(
            get("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
          storages <- ZIO.fromEither(decode[Vector[StorageSummaryResp]](bodyStr))
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storages.length == storageNames.length)
           && assertTrue(storages.map(_.name).forall(storageNames.contains))
           && assertTrue(storageNames.forall(storages.map(_.name).contains))
      },
      test("When there are only other user's storages should get 200 and no storages") {
        for
          n <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          ownedStorageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          m <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          memberedStorageNames <- storageNameGen.sample.map(_.value).take(m).runCollect
          storageNames = ownedStorageNames ++ memberedStorageNames

          memberId <- registerUser
          _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(ownedStorageNames){ name => repo.createEmpty(name, memberId) }
          }

          creatorId <- registerUser
          memberedStorageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(memberedStorageNames){ name => repo.createEmpty(name, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(memberedStorageIds){ id => repo.addMemberToStorageById(id, memberId) }
          }

          userId <- registerUser(creatorId + memberId)

          resp <- Client.batched(
            get("my/storages")
              .withJsonBody(CreateStorageReqBody("storage"))
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(bodyStr == "[]")
      },
    ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] =
    val baseNames = Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf", "Кухня", "Шкаф", "Холодильник", "Морозилка", "Полка", "Общежитие", "Кампус", "Подвал")
    val suffixes = Gen.elements("", "-1", "-2", "-Main", "-Backup", "-Запас", "-Основа")
    for
      base <- baseNames
      suffix <- suffixes
    yield base + suffix
