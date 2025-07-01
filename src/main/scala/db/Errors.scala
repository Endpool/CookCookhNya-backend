package db

import java.sql.SQLException

enum DbError(val message: String):
  case FailedDbQuery(sqlExc: SQLException) extends DbError(s"Failed to execute DB query: ${sqlExc.getMessage}")
  case DbNotRespondingError(msg: String) extends DbError(s"DB connection failed: $msg")
