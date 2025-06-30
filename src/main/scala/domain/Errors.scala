package domain

sealed trait ErrorResponse:
  val message: String

enum IngredientError(val message: String) extends ErrorResponse:
  case NotFound(ingredientId: String) extends IngredientError(s"No ingredient with id $ingredientId")

enum StorageError(val message: String) extends ErrorResponse:
  case NotFound(storageId: String) extends StorageError(s"No storage with id $storageId")
  
enum UserError(val message: String) extends ErrorResponse:
  case NotFound(userId: String) extends UserError(s"No user with id $userId")

enum RecipeError(val message: String) extends ErrorResponse:
  case NotFound(recipeId: String) extends RecipeError(s"No user with id $RecipeError")

case class InternalServerError(message: String = "Something went wrong on the server side") extends ErrorResponse
