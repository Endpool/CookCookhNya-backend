package integration.api.recipes

import api.Main
import api.recipes.getSuggested
import db.createTables
import db.DataSourceDescription
import db.dbLayer
import db.repositories.*
import domain.UserId

import com.augustnagro.magnum.magzio.sql
import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.*
import zio.http.*
import zio.http.Request.*
import zio.test.*
import zio.test.Assertion.*

case class ServerFiber(server: Fiber.Runtime[Throwable, Nothing])

extension(req: Request)
  def addAuthorization(userId: UserId) =
    req.addHeader("Authorization", s"Bearer $userId")

object IntegrationTest extends ZIOSpecDefault:
  val serverPort = 8080
  val serverUrl: URL = URL.decode(s"http://localhost:$serverPort/").toOption.get

  def server(dataSourceDescr: DataSourceDescription): ZIO[Any, Throwable, Nothing] =
    Server.serve(Main.app)
      .provide(
        ZLayer.succeed(Server.Config.default.port(serverPort)),
        dbLayer(dataSourceDescr),
        IngredientsRepoLive.layer,
        UsersRepo.layer,
        StoragesRepoLive.layer,
        StorageIngredientsRepoLive.layer,
        StorageMembersRepoLive.layer,
        RecipesRepo.layer,
        RecipeIngredientsRepo.layer,
        Server.live
      )

  override def spec =
    test("get my storages returns status Ok") {
      val userId = 52
      for
        resp <- Client.batched(
          get(serverUrl / "my" / "storages")
            .addAuthorization(userId)
        )
      yield assertTrue(resp.status == Status.Ok)
    } .provideSomeLayer(Client.default)
      .provideSomeLayerShared(serverLayer)
      .provideSomeLayer(psqlContainerLayer)

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

  def serverLayer: URLayer[PostgreSQLContainer, ServerFiber] = ZLayer.scoped {
    for
      container <- ZIO.service[PostgreSQLContainer]
      dataSourceDescr = DataSourceDescription(
        container.jdbcUrl,
        container.username,
        container.password,
        container.driverClassName
      )
      server <- server(dataSourceDescr).forkScoped
    yield ServerFiber(server)
  }
