package domain

final case class IngredientNotFound(ingredientId: String):
  val message = s"No ingredient with id $ingredientId"

final case class StorageNotFound(storageId: String):
  val message = s"No storage with id $storageId"
final case class StorageAccessForbidden(storageId: String):
  val message = s"Access to storage with id $storageId is forbidden"

final case class UserNotFound(userId: String):
  val message = s"No user with id $userId"

final case class RecipeNotFound(recipeId: String):
  val message = s"No recipe with id $recipeId"

final case class InternalServerError(message: String = "Something went wrong on the server side")

final case class InvalidInvitationHash(hash: String):
  val message = s"Invalid invitation hash: $hash"
