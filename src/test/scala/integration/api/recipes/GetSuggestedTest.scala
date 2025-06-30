import zio.test.*
import zio.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.augustnagro.magnum.magzio.sql
import com.augustnagro.magnum.magzio.Transactor
import db.createTables
import db.dbLayer
import api.recipes.getSuggested
import db.DataSourceDescription

object IntegrationTest extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("aboba") {
      test("arbuz") {
        ZIO.scoped {
          for
            container <- getContainer
            dataSourceDescr = DataSourceDescription(
              container.jdbcUrl,
              container.username,
              container.password
            )
            xa <- dbLayer(dataSourceDescr).build
          yield assertTrue(true)
        }
      }
    }

  val getContainer: ZIO[Scope, Throwable, PostgreSQLContainer] =
    ZIO.acquireRelease(
      ZIO.attempt {
        val container = PostgreSQLContainer()
        container.start()
        container
      }
    ){ container =>
      ZIO.attempt(container.stop()).orDie
    }
