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
  private val endpointPath: String = "my/storages"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Get all storages tests")(
      test("When unauthorized should get 401") {
        for
          resp <- Client.batched(get(endpointPath))
        yield assertTrue(resp.status == Status.Unauthorized)
      },
      test("When authorized with no storages should get 200 and empty list") {
        for
          userId <- registerUser
          resp <- Client.batched(
            get(endpointPath)
              .addAuthorization(userId)
          )
          bodyStr <- resp.body.asString
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(bodyStr == "[]")
      },
      test("When authorized with owned storages should get 200 and all storages") {
        for
          userId <- registerUser
          n <- Gen.int(1, 10).runHead.map(_.getOrElse(4))
          storageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(storageNames){ repo.createEmpty(_, userId) }
          }

          resp <- Client.batched(
            get(endpointPath)
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
      test("When authorized with membered storages should get 200 and all storages") {
        for
          creatorId <- registerUser
          userId <- registerUser
          n <- Gen.int(1, 10).runHead.map(_.getOrElse(4))
          storageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          storageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(storageNames){ repo.createEmpty(_, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(storageIds){ repo.addMemberToStorageById(_, userId) }
          }

          resp <- Client.batched(
            get(endpointPath)
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
          storages <- ZIO.fromEither(decode[Vector[StorageSummaryResp]](bodyStr))
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storages.length == storageNames.length)
           && assertTrue(storages.map(_.name).forall(storageNames.contains))
           && assertTrue(storageNames.forall(storages.map(_.name).contains))
      },
      test("When authorized with owned and membered storages should return 200 with all storages") {
        for
          n <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          ownedStorageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
          m <- Gen.int(1, 5).runHead.map(_.getOrElse(2))
          memberedStorageNames <- storageNameGen.sample.map(_.value).take(m).runCollect
          storageNames = ownedStorageNames ++ memberedStorageNames

          userId <- registerUser
          _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(ownedStorageNames){ repo.createEmpty(_, userId) }
          }

          creatorId <- registerUser
          memberedStorageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(memberedStorageNames){ repo.createEmpty(_, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(memberedStorageIds){ repo.addMemberToStorageById(_, userId) }
          }

          resp <- Client.batched(
            get(endpointPath)
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
            ZIO.foreach(ownedStorageNames){ repo.createEmpty(_, memberId) }
          }

          creatorId <- registerUser
          memberedStorageIds <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
            ZIO.foreach(memberedStorageNames){ repo.createEmpty(_, creatorId) }
          }
          _ <- ZIO.serviceWithZIO[StorageMembersRepo]{ repo =>
            ZIO.foreach(memberedStorageIds){ repo.addMemberToStorageById(_, memberId) }
          }

          userId <- registerUser(creatorId + memberId)

          resp <- Client.batched(
            get(endpointPath)
              .addAuthorization(userId)
          )

          bodyStr <- resp.body.asString
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(bodyStr == "[]")
      },
    ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] = for
    base <- Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf", "Кухня", "Шкаф", "Холодильник", "Морозилка", "Полка", "Общежитие", "Кампус", "Подвал")
    suffix <- Gen.elements("", "-1", "-2", "-Main", "-Backup", "-Запас", "-Основа")
  yield base + suffix
