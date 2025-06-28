package db.repositories

import domain.DbError
import zio.ZIO

extension[R, A](zioInstance: ZIO[R, Throwable, A])
  def catchAllAsDbError: ZIO[R, DbError.UnexpectedDbError, A] = zioInstance
    .catchAll(e => ZIO.fail(DbError.UnexpectedDbError(e.getMessage)))