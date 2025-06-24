package db

import domain.DbError
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}

import org.postgresql.util.PSQLException

def handleDbError(error: Throwable): DbError =
  val errorCause = error.getCause
  val errorMessage = errorCause.getMessage

  errorCause match
    case exc: PSQLException => FailedDbQuery(exc)
    case _: java.sql.SQLTransientConnectionException => DbNotRespondingError(errorMessage)
    case _ => UnexpectedDbError(errorCause.toString + ": " + errorMessage)

def handleUnfailableQuery(error: DbError): UnexpectedDbError | DbNotRespondingError =
  error match
    case FailedDbQuery(exc) => UnexpectedDbError(exc.getMessage)
    case e: (UnexpectedDbError | DbNotRespondingError) => e

def getAbsentKey(excDetail: String): Option[String] =
  val pattern = """Key \((.*)\)=\((.*)\) is not present in table "(.*)".""".r
  excDetail match
    case pattern(key, _, _) => Some(key)
    case _ => None
