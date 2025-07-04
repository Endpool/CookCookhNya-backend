package integration.api.storages

import api.storages.StorageSummaryResp
import db.repositories.{StorageMembersRepo, StoragesRepo}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.decode
import zio.http.{Client, Status, URL, Path}
import zio.http.Request.get
import zio.{Scope, ZIO}
import zio.test.{Gen, TestEnvironment, assertTrue, Spec}

object GetAllStoragesTests extends ZIOIntegrationTestSpec:
  private val endpointPath: URL = URL(Path.root / "my" / "storages")

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Get all storages tests")(
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
        n <- Gen.int(1, 10).runHead.some
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
         && assertTrue(storages.map(_.name).hasSameElementsAs(storageNames))
         && assertTrue(storages.forall(_.ownerId == userId))
    },
    test("When authorized with membered storages should get 200 and all storages") {
      for
        creatorId <- registerUser
        userId <- registerUser
        n <- Gen.int(1, 10).runHead.some
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
         && assertTrue(storages.map(_.name).hasSameElementsAs(storageNames))
    },
    test("When authorized with owned and membered storages should return 200 with all storages") {
      for
        n <- Gen.int(1, 5).runHead.some
        ownedStorageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
        m <- Gen.int(1, 5).runHead.some
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
         && assertTrue(storages.map(_.name).hasSameElementsAs(storageNames))
    },
    test("When there are only other user's storages should get 200 and no storages") {
      for
        n <- Gen.int(1, 5).runHead.some
        ownedStorageNames <- storageNameGen.sample.map(_.value).take(n).runCollect
        m <- Gen.int(1, 5).runHead.some
        memberedStorageNames <- storageNameGen.sample.map(_.value).take(m).runCollect
        storageNames = ownedStorageNames ++ memberedStorageNames

        memberId <- registerUser
        _ <- ZIO.serviceWithZIO[StoragesRepo]{ repo =>
          ZIO.foreach(ownedStorageNames)(repo.createEmpty(_, memberId))
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
    test("When authorized with multiple users, cross-membered storages, and owned/membered storages should return 200 with all available storages") {
      for {
        userA <- registerUser // Test user
        userB <- registerUser
        userC <- registerUser

        storagesPerUser <- Gen.int(1, 4).runHead.some
        userAOwnedNames <- storageNameGen.sample.map(_.value).take(storagesPerUser).runCollect
        userBOwnedNames <- storageNameGen.sample.map(_.value).take(storagesPerUser).runCollect
        userCOwnedNames <- storageNameGen.sample.map(_.value).take(storagesPerUser).runCollect

        usersStoragesIds <- ZIO.serviceWithZIO[StoragesRepo] { repo =>
          for
            a <- ZIO.foreach(userAOwnedNames) { repo.createEmpty(_, userA) }
            b <- ZIO.foreach(userBOwnedNames) { repo.createEmpty(_, userB) }
            c <- ZIO.foreach(userCOwnedNames) { repo.createEmpty(_, userC) }
          yield (a, b, c)
        }
        (userAStorageIds, userBStorageIds, userCStorageIds) = usersStoragesIds

        userAMemberedStorageIds = Seq(userBStorageIds.head, userCStorageIds.last)
        _ <- ZIO.serviceWithZIO[StorageMembersRepo] { repo
          // User A is a member of one of User B's storages
          => ZIO.foreach(userAMemberedStorageIds){ repo.addMemberToStorageById(_, userA) }
          // User B is a member of one of User A's storages
          *> repo.addMemberToStorageById(userAStorageIds.head, userB)
          // User B is a member of one of User C's storages
          *> repo.addMemberToStorageById(userCStorageIds.head, userB)
          // User C is a member of one of User B's storages
          *> repo.addMemberToStorageById(userBStorageIds.last, userC)
        }

        userAMemberedNames <- ZIO.serviceWithZIO[StoragesRepo] { repo =>
          ZIO.foreach(userAMemberedStorageIds)(repo.getById)
            .map(_.flatten.map(_.name))
        }
        expectedStorageNames = userAOwnedNames ++ userAMemberedNames

        resp <- Client.batched(
          get(endpointPath)
            .addAuthorization(userA)
        )

        bodyStr <- resp.body.asString
        storages <- ZIO.fromEither(decode[Vector[StorageSummaryResp]](bodyStr))
      } yield assertTrue(resp.status == Status.Ok)
           && assertTrue(storages.map(_.name).hasSameElementsAs(expectedStorageNames))
    }
  ).provideLayer(testLayer)

  val storageNameGen: Gen[Any, String] = for
    base <- Gen.elements("Pantry", "Fridge", "Freezer", "Cupboard", "Shelf", "Кухня", "Шкаф", "Холодильник", "Морозилка", "Полка", "Общежитие", "Кампус", "Подвал")
    suffix <- Gen.elements("", "-1", "-2", "-Main", "-Backup", "-Запас", "-Основа")
  yield base + suffix
