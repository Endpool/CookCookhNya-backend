package backend

sealed trait ErrorResponse:
  val message: String

enum IngredientError(val message: String) extends ErrorResponse:
  case IngredientNotFound(ingredientId: IngredientId) extends IngredientError(s"No ingredient with id $ingredientId")

enum StorageError(val message: String) extends ErrorResponse:
  case StorageNotFound(storageId: StorageId) extends StorageError(s"No storage with id $storageId")