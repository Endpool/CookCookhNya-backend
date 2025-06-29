package db.repositories

import domain.DbError
import zio.ZIO

extension[R, A](zioInstance: ZIO[R, Throwable, A])
  def catchAllAsDbError: ZIO[R, DbError.UnexpectedDbError, A] =
    zioInstance.catchAll {
      case e: DbError.UnexpectedDbError => ZIO.fail(e)
      case other => ZIO.fail(DbError.UnexpectedDbError(other.getMessage))
    }