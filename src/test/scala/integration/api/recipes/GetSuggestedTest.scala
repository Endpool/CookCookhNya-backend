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

object IntegrationTest extends ZIOSpecDefault:
  def server(dataSourceDescr: DataSourceDescription) =
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
    suite("Test tests") {
      test("get my storages returns status Ok") {
        for
          resp <- Client.request(
            Request.get("http://localhost:8080/my/storages")
              .addHeader("Authorization", "Bearer 52")
          )
        yield assert(resp.status)(equalTo(Status.Ok))
      }
    }.provideSomeLayer {
      ZLayer.scoped {
        for
          container <- getContainer
          dataSourceDescr = DataSourceDescription(
            container.jdbcUrl,
            container.username,
            container.password,
            container.driverClassName
          )
          fiber <- server(dataSourceDescr).forkDaemon
          client <- ZIO.service[Client].provide(Client.default)
        yield client
      } ++ ZLayer.succeed(Scope.global)
    }

  val getContainer: ZIO[Scope, Throwable, PostgreSQLContainer] =
    ZIO.acquireRelease(
      ZIO.attempt {
        val container = PostgreSQLContainer()
        container.start
        container
      }
    ){ container =>
      ZIO.attempt{ container.stop }.orDie
    }
