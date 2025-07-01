import api.Main
import api.recipes.getSuggested
import com.augustnagro.magnum.magzio.sql
import com.augustnagro.magnum.magzio.Transactor
import com.dimafeng.testcontainers.PostgreSQLContainer
import db.createTables
import db.DataSourceDescription
import db.dbLayer
import db.repositories.*
import zio.*
import zio.http.*
import zio.test.*
import zio.test.Assertion.*

case class ServerFiber(server: Fiber.Runtime[Throwable, Nothing])

object IntegrationTest extends ZIOSpecDefault:
  def server(dataSourceDescr: DataSourceDescription): ZIO[Any, Throwable, Nothing] =
    Server.serve(Main.app)
      .provide(
        ZLayer.succeed(Server.Config.default.port(8080)),
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
      for
        resp <- Client.batched(
          Request.get("http://localhost:8080/my/storages")
            .addHeader("Authorization", "Bearer 52")
        ).provide(Client.default)
      yield assertTrue(resp.status == Status.Ok)
    }.provideSomeLayerShared(serverLayer)
     .provideSomeLayer(psqlContainerLayer)

  val psqlContainerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] =
    ZLayer.scoped {
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

  def serverLayer: ZLayer[PostgreSQLContainer, Nothing, ServerFiber] = ZLayer.scoped {
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
