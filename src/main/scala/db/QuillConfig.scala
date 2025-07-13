package db

import io.getquill.{LowerCase, NamingStrategy, PluralizedTableNames, PostgresZioJdbcContext, SnakeCase, StripTableDbPrefix}
import javax.sql.DataSource
import zio.{ZIO, ZLayer}

object QuillConfig:
  val ctx = PostgresZioJdbcContext(NamingStrategy(StripTableDbPrefix, SnakeCase, PluralizedTableNames, LowerCase))

  extension[R, A](zio: ZIO[DataSource & R, Throwable, A])
    def provideDS(using dataSource: DataSource): ZIO[R, DbError, A] = zio
      .provideSomeLayer(ZLayer.succeed(dataSource))
      .mapError(handleDbError)
