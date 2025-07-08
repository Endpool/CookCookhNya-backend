package domain

sealed trait ErrorResponse:
  val message: String

final case class IngredientNotFound(ingredientId: String) extends ErrorResponse:
  val message = s"No ingredient with id $ingredientId"

final case class StorageNotFound(storageId: String) extends ErrorResponse:
  val message = s"No storage with id $storageId"
final case class StorageAccessForbidden(storageId: String) extends ErrorResponse:
  val message = s"Access to storage with id $storageId is forbidden"

final case class UserNotFound(userId: String) extends ErrorResponse:
  val message = s"No user with id $userId"

final case class RecipeNotFound(recipeId: String) extends ErrorResponse:
  val message = s"No recipe with id $recipeId"

final case class InternalServerError(message: String = "Something went wrong on the server side") extends ErrorResponse
