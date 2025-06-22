package domain

sealed trait ErrorResponse:
  val message: String

enum IngredientError(val message: String) extends ErrorResponse:
  case NotFound(ingredientId: IngredientId) extends IngredientError(s"No ingredient with id $ingredientId")

enum StorageError(val message: String) extends ErrorResponse:
  case NotFound(storageId: StorageId) extends StorageError(s"No storage with id $storageId")
  
enum UserError(val message: String) extends ErrorResponse:
  case NotFound(userId: UserId) extends UserError(s"No user with id $userId")

enum RecipeError(val message: String) extends ErrorResponse:
  case NotFound(recipeId: RecipeId) extends RecipeError(s"No user with id $RecipeError")

enum DbError(val message: String) extends ErrorResponse:
  case UnexpectedDbError(msg: String) extends DbError(s"Something went wrong with the db: $msg")