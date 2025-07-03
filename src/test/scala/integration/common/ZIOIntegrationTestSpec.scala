package integration.common

import api.Main
import api.users.CreateUserReqBody
import db.{DataSourceDescription, dbLayer}
import db.repositories.{
  IngredientsRepo,
  RecipeIngredientsRepo,
  RecipesDomainRepo,
  RecipesRepo,
  ShoppingListsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
}
import integration.common.Utils.{put, withJsonBody}

import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.generic.auto.deriveEncoder
import zio.{ZLayer, RLayer, URLayer, TaskLayer, ZIO, RIO, ZEnvironment}
import zio.http.{Client, Server, TestServer, URL}
import zio.test.ZIOSpecDefault
import integration.common.Utils.addAuthorization
import domain.UserId
import zio.test.Gen

abstract class ZIOIntegrationTestSpec extends ZIOSpecDefault:
  protected def testLayer:
    TaskLayer[
      Client
      & IngredientsRepo
      & StorageMembersRepo
      & StoragesRepo
      & RecipeIngredientsRepo
      & RecipesRepo
      & StorageIngredientsRepo
      & UsersRepo
    ] =
    psqlContainerLayer >>> dataSourceDescritptionLayer >>> dbLayer >>> (
      testServerLayer >>> clientLayer
      ++ IngredientsRepo.layer
      ++ RecipeIngredientsRepo.layer
      ++ RecipesRepo.layer
      ++ StorageIngredientsRepo.layer
      ++ StorageMembersRepo.layer
      ++ StoragesRepo.layer
      ++ UsersRepo.layer
    )

  protected def registerUser: RIO[Client, UserId] =
    Gen.long(1, 100000000)
      .runHead.map(_.getOrElse(52L))
      .flatMap(registerUser(_))
      .flatMap(registerUser)

  protected def registerNUsers(n: Int): RIO[Client, Array[UserId]] =
    ZIO.collectAll(
      (1 to n).map(_ => registerUser).toArray
    )

  protected def registerUser(userId: UserId): RIO[Client, UserId] = for
    alias <- Gen.alphaNumericStringBounded(3, 13).runHead
    fullName <- Gen.alphaNumericStringBounded(3, 13).runHead.map(_.getOrElse("fullName"))
    resp <- Client.batched(
      put("users")
        .withJsonBody(CreateUserReqBody(alias, fullName))
        .addAuthorization(userId)
    )
    _ <- resp.body.asString
  yield userId

  protected def registerUser(
    userId: UserId,
    alias: Option[String],
    fullName: String,
  ): RIO[Client, UserId] = for
    resp <- Client.batched(
      put("users")
        .withJsonBody(CreateUserReqBody(alias, fullName))
        .addAuthorization(userId)
    )
    _ <- resp.body.asString
  yield userId

  private val psqlContainerLayer: TaskLayer[PostgreSQLContainer] = ZLayer.scoped {
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

  private val dataSourceDescritptionLayer: URLayer[PostgreSQLContainer, DataSourceDescription] =
    ZLayer.fromFunction { (container: PostgreSQLContainer) =>
      DataSourceDescription(
        container.jdbcUrl,
        container.username,
        container.password,
        container.driverClassName
      )
    }

  private def initTestServer(testServer: TestServer): RIO[Transactor, Unit] =
    testServer.addRoutes(Main.app)
      .provideSomeLayer(
        IngredientsRepo.layer ++
        RecipeIngredientsRepo.layer ++
        RecipesDomainRepo.layer ++
        RecipesRepo.layer ++
        ShoppingListsRepo.layer ++
        StorageIngredientsRepo.layer ++
        StorageMembersRepo.layer ++
        StoragesRepo.layer ++
        UsersRepo.layer
      )

  private val testServerLayer: RLayer[Transactor, TestServer] = for
    testServer <- TestServer.default
    _ <- ZLayer.fromZIO(initTestServer(testServer.get))
  yield testServer

  private val clientLayer: RLayer[Server, Client] = for
    port <- ZLayer(ZIO.serviceWithZIO[Server](_.port))
    client <- Client.default
    url <- ZLayer(ZIO.fromEither(URL.decode(s"http://localhost:${port.get}/")))
  yield ZEnvironment(client.get.url(url.get))
