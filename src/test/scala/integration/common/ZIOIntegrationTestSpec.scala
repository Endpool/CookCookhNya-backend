package integration.common

import api.Main
import db.{DataSourceDescription, dataSourceLayer, dbLayer}
import db.repositories.*

import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import javax.sql.DataSource
import org.testcontainers.utility.DockerImageName
import zio.http.{Client, Server, TestServer, URL}
import zio.test.ZIOSpecDefault
import zio.{ZLayer, RLayer, URLayer, TaskLayer, ZIO, RIO, ZEnvironment}

abstract class ZIOIntegrationTestSpec extends ZIOSpecDefault:
  private val postgreSQLContainerTag: String = "17"

  protected def testLayer:
    TaskLayer[
      Client
      & Transactor & DataSource
      & IngredientsRepo
      & InvitationsRepo
      & RecipeIngredientsRepo
      & RecipesRepo
      & RecipePublicationRequestsRepo
      & StorageIngredientsRepo
      & StorageMembersRepo
      & StoragesRepo
      & UsersRepo
    ] =
    psqlContainerLayer >>> dataSourceDescritptionLayer >>> dataSourceLayer >+> dbLayer >+> (
      testServerLayer >>> clientLayer ++ testReposLayer
    )


  private val psqlContainerLayer: TaskLayer[PostgreSQLContainer] = ZLayer.scoped {
    ZIO.acquireRelease(
      ZIO.attempt {
        val container = PostgreSQLContainer(DockerImageName(s"postgres:$postgreSQLContainerTag"))
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

  private def initTestServer(testServer: TestServer): RIO[DataSource & Transactor, Unit] =
    testServer.addRoutes(Main.app)
      .provideSomeLayer(testReposLayer)

  private val testServerLayer: RLayer[DataSource & Transactor, TestServer] = for
    testServer <- TestServer.default
    _ <- ZLayer.fromZIO(initTestServer(testServer.get))
  yield testServer

  private val clientLayer: RLayer[Server, Client] = for
    port <- ZLayer(ZIO.serviceWithZIO[Server](_.port))
    client <- Client.default
    url <- ZLayer(ZIO.fromEither(URL.decode(s"http://localhost:${port.get}/")))
  yield ZEnvironment(client.get.url(url.get))
