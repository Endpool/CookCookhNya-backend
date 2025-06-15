package backend

sealed abstract class ErrorResponse(message: String)

sealed case class StorageError(message: String) extends ErrorResponse(message)
object StorageNotFound:
  def apply(): StorageError = StorageError("Storage not found")

sealed case class IngredientError(message: String) extends ErrorResponse(message)
object IngredientNotFound:
  def apply(): IngredientError = IngredientError("Ingredient not found")
