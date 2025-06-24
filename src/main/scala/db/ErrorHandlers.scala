package db

import domain.DbError

def handleDbError(error: Throwable): DbError =
  val errorCause = error.getCause
  val errorMessage = error.getCause.getMessage
  
  println(s"Error class: ${errorCause.getClass}")
  println(s"Error message: $errorMessage")

  errorCause match
    case _: org.postgresql.util.PSQLException => DbError.FailedDbQuery(errorMessage)
    case _: java.sql.SQLTransientConnectionException => DbError.DbNotRespondingError(errorMessage)
    case _ => DbError.UnexpectedDbError(errorCause.toString + ": " + errorMessage)
