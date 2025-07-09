package integration.common

import api.Main
import api.users.CreateUserReqBody
import db.{DataSourceDescription, dbLayer}
import db.repositories.{
  IngredientsRepo,
  InvitationsRepo,
  InvitationsSecretKey,
  RecipeIngredientsRepo,
  RecipesDomainRepo,
  RecipesRepo,
  ShoppingListsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
}
import domain.UserId
import integration.common.Utils.{addAuthorization, put, withJsonBody}

import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.generic.auto.deriveEncoder
import zio.http.{Client, Server, TestServer, URL}
import zio.test.Gen
import zio.test.ZIOSpecDefault
import zio.{ZLayer, RLayer, URLayer, TaskLayer, ZIO, RIO, ZEnvironment}

abstract class ZIOIntegrationTestSpec extends ZIOSpecDefault:
  protected def testLayer:
    TaskLayer[
      Client
      & Transactor
      & IngredientsRepo
      & InvitationsRepo
      & RecipeIngredientsRepo
      & RecipesRepo
      & StorageIngredientsRepo
      & StorageMembersRepo
      & StoragesRepo
      & UsersRepo
    ] =
    psqlContainerLayer >>> dataSourceDescritptionLayer >>> dbLayer >+> (
      testServerLayer >>> clientLayer ++ testReposLayer
    )


  private val psqlContainerLayer: TaskLayer[PostgreSQLContainer] = ZLayer.scoped {
    ZIO.acquireRelease(
      ZIO.attempt {
        val container = PostgreSQLContainer()
        container.start
        container
      }
    )(container => ZIO.attemptBlocking(container.stop).orDie)
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

  private val testReposLayer =
    ZLayer.succeed(InvitationsSecretKey("Invit4ti0n553c7etK3yInv1t4t10n5Secret3ey")) >>> Main.reposLayer

  private def initTestServer(testServer: TestServer): RIO[Transactor, Unit] =
    testServer.addRoutes(Main.app)
      .provideSomeLayer(testReposLayer)

  private val testServerLayer: RLayer[Transactor, TestServer] = for
    testServer <- TestServer.default
    _ <- ZLayer.fromZIO(initTestServer(testServer.get))
  yield testServer

  private val clientLayer: RLayer[Server, Client] = for
    port <- ZLayer(ZIO.serviceWithZIO[Server](_.port))
    client <- Client.default
    url <- ZLayer(ZIO.fromEither(URL.decode(s"http://localhost:${port.get}/")))
  yield ZEnvironment(client.get.url(url.get))
