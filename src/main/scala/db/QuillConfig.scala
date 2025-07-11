package db

import io.getquill.{LowerCase, NamingStrategy, PluralizedTableNames, PostgresZioJdbcContext, SnakeCase, StripTableDbPrefix}
import java.sql.SQLException
import javax.sql.DataSource
import zio.{ZIO, ZEnvironment}

object QuillConfig:
  val ctx = PostgresZioJdbcContext(NamingStrategy(StripTableDbPrefix, SnakeCase, PluralizedTableNames, LowerCase))

  extension[E, A](zio: ZIO[DataSource, Throwable, A])
    def provideDS(using dataSource: DataSource) = zio
      .provideEnvironment(ZEnvironment(dataSource))
      .mapError(handleDbError)
