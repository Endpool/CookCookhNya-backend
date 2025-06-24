package domain

import org.postgresql.util.PSQLException

sealed trait ErrorResponse:
  val message: String

enum IngredientError(val message: String) extends ErrorResponse:
  case NotFound(ingredientId: IngredientId) extends IngredientError(s"No ingredient with id $ingredientId")

enum StorageError(val message: String) extends ErrorResponse:
  case NotFound(storageId: StorageId) extends StorageError(s"No storage with id $storageId")
  
enum UserError(val message: String) extends ErrorResponse:
  case NotFound(userId: UserId) extends UserError(s"No user with id $userId")

enum DbError(val message: String) extends ErrorResponse:
  case UnexpectedDbError(msg: String) extends DbError(s"Something went wrong with the db: $msg")
  case FailedDbQuery(sqlExc: PSQLException) extends DbError(s"Failed to execute DB query: ${sqlExc.getServerErrorMessage}")
  case DbNotRespondingError(msg: String) extends DbError(s"DB connection failed: $msg")
