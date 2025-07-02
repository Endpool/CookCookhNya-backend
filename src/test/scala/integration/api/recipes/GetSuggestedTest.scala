package integration.api.recipes

import api.Main
import api.recipes.getSuggested
import api.storages.CreateStorageReqBody
import api.users.CreateUserReqBody
import common.Utils.*
import db.{DataSourceDescription, dbLayer}
import db.repositories.*
import domain.{StorageId, UserId}

import com.augustnagro.magnum.magzio.sql
import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.Encoder
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import zio.*
import zio.http.*
import zio.http.Request.{get}
import zio.test.*
import zio.test.Assertion.*

object IntegrationTest extends ZIOSpecDefault:
  def auth(userId: UserId, reqBody: CreateUserReqBody): RIO[Server & Client, Unit] = for
    _ <- Client.batched(
      put("users")
        .withJsonBody(reqBody)
        .addAuthorization(userId)
    )
  yield ()

  override def spec: Spec[Environment & TestEnvironment & Scope, Any] =
    suite("aboba")(
      test("get my storages returns status Ok") {
        val userId = 52
        for
          resp <- Client.batched(
            get("my/storages")
              .addAuthorization(userId)
          )
        yield assertTrue(resp.status == Status.Ok)
      },
      test("test test") {
        val userId = 52
        val reqBody = CreateUserReqBody(None, "cuckookhnya")
        for
          resp <- Client.batched(
            put("users")
              .withJsonBody(reqBody)
              .addAuthorization(userId)
          )
        yield assertTrue(resp.status == Status.Ok)
      },
      test("Auth test") {
        val userId = 52
        for
          resp <- Client.batched(
            get("my/storages")
          )
        yield assertTrue(resp.status == Status.Unauthorized)
      },
      test("Auth test") {
        val userId = 52
        val aboa = CreateUserReqBody(None, "cuckookhnya")
        var reqBody = CreateStorageReqBody("aboba")
        for
          _ <- auth(userId, aboa)
          resp <- Client.batched(
            post("my/storages")
              .withJsonBody(reqBody)
              .addAuthorization(userId)
          )
          bodyStr <- resp.body.asString
          storageId <- ZIO.fromEither(decode[StorageId](bodyStr))
          storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
        yield assertTrue(storage.is(_.some).id == storageId)
           && assertTrue(storage.is(_.some).ownerId == userId)
           && assertTrue(storageId == 1)
      }
    ).provide(
      clientLayer,
      StoragesRepoLive.layer,
      dbLayer,
      dataSourceDescritptionLayer,
      psqlContainerLayer,
      testServerLayer,
    )

  val psqlContainerLayer: TaskLayer[PostgreSQLContainer] = ZLayer.scoped {
    ZIO.acquireRelease(
      ZIO.attempt {
        val container = PostgreSQLContainer()
        container.start
        container
      }
    ){ container =>
      ZIO.attemptBlocking{ container.stop }.orDie
    }
  }

  val dataSourceDescritptionLayer: URLayer[PostgreSQLContainer, DataSourceDescription] =
    ZLayer.fromFunction { (container: PostgreSQLContainer) =>
      DataSourceDescription(
        container.jdbcUrl,
        container.username,
        container.password,
        container.driverClassName
      )
    }

  def initTestServer(testServer: TestServer): RIO[Transactor, Unit] =
    testServer.addRoutes(Main.app)
      .provideSomeAuto(
        IngredientsRepoLive.layer,
        UsersRepo.layer,
        StoragesRepoLive.layer,
        StorageIngredientsRepoLive.layer,
        StorageMembersRepoLive.layer,
        RecipesRepo.layer,
        RecipeIngredientsRepo.layer,
      )

  val testServerLayer: RLayer[Transactor, TestServer] = for
    testServer <- TestServer.default
    _ <- ZLayer.fromZIO(initTestServer(testServer.get))
  yield testServer

  val clientLayer: RLayer[Server, Client] = for
    port <- ZLayer(ZIO.serviceWithZIO[Server](_.port))
    client <- Client.default
    url <- ZLayer(ZIO.fromEither(URL.decode(s"http://localhost:${port.get}/")))
  yield ZEnvironment(client.get.url(url.get))
