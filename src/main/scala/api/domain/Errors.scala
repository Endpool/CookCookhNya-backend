package api.domain

sealed trait ErrorResponse:
  val message: String

enum IngredientError(val message: String) extends ErrorResponse:
  case NotFound(ingredientId: IngredientId) extends IngredientError(s"No ingredient with id $ingredientId")

enum StorageError(val message: String) extends ErrorResponse:
  case NotFound(storageId: StorageId) extends StorageError(s"No storage with id $storageId")
