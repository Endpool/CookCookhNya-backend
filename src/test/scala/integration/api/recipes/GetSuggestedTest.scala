import zio.test.*
import zio.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.augustnagro.magnum.magzio.sql
import com.augustnagro.magnum.magzio.Transactor
import db.createTables
import db.dbLayer
import api.recipes.getSuggested

object IntegrationTest extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("aboba") {
      test("arbuz") {
        ZIO.scoped {
          for
            container <- getContainer
            dbName <- getDbName
            xa <- dbLayer(ZIO.succeed(???)).build
            _ <- createTables(xa.get)
          yield assertTrue(true == true)
        }
      }
    }

  val getDbName: Task[String] =
    ZIO.attempt(java.util.UUID.randomUUID().toString.replace("-", ""))

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
