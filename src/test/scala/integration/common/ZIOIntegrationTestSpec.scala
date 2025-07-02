package integration.common

import api.Main
import db.{DataSourceDescription, dbLayer}
import db.repositories.{
  IngredientsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
  RecipeIngredientsRepo,
  RecipesRepo
}

import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.{ZLayer, RLayer, URLayer, TaskLayer, ZIO, RIO, ZEnvironment}
import zio.http.{Client, Server, TestServer, URL}
import zio.test.ZIOSpecDefault

abstract class ZIOIntegrationTestSpec extends ZIOSpecDefault:
  def testLayer:
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
        RecipesRepo.layer ++
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
