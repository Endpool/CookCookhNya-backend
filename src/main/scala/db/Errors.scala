package db

import org.postgresql.util.PSQLException

enum DbError(val message: String):
  case FailedDbQuery(sqlExc: PSQLException) extends DbError(s"Failed to execute DB query: ${sqlExc.getServerErrorMessage}")
  case DbNotRespondingError(msg: String) extends DbError(s"DB connection failed: $msg")
