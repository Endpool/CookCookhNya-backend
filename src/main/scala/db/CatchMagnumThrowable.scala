package db

import db.DbError.{DbNotRespondingError, FailedDbQuery}

import java.sql.BatchUpdateException
import org.postgresql.util.PSQLException

def handleDbError(error: Throwable): DbError =
  val errorCause = error.getCause
  val errorMessage = errorCause.getMessage

  errorCause match
    case exc: (PSQLException | BatchUpdateException) => FailedDbQuery(exc)
    case _: java.sql.SQLTransientConnectionException => DbNotRespondingError(errorMessage)
