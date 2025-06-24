package db

import domain.DbError
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}

def handleDbError(error: Throwable): DbError =
  val errorCause = error.getCause
  val errorMessage = errorCause.getMessage

  errorCause match
    case _: org.postgresql.util.PSQLException => FailedDbQuery(errorMessage)
    case _: java.sql.SQLTransientConnectionException => DbNotRespondingError(errorMessage)
    case _ => UnexpectedDbError(errorCause.toString + ": " + errorMessage)

def handleUnfailableQuery(error: DbError): UnexpectedDbError | DbNotRespondingError =
  error match
    case FailedDbQuery(msg) => UnexpectedDbError(msg)
    case e: (UnexpectedDbError | DbNotRespondingError) => e
