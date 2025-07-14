package domain

final case class IngredientNotFound(
  ingredientId: String,
  message: String = "Ingredient not found",
)

final case class StorageNotFound(
  storageId: String,
  message: String = "Storage not found",
)
final case class StorageAccessForbidden(
  storageId: String,
  message: String = "Access to the storage is forbidden",
)

final case class UserNotFound(
  userId: String,
  message: String = "User not found",
)

final case class RecipeNotFound(
  recipeId: String,
  message: String = "Recipe not found",
)
final case class RecipeAccessForbidden(
  recipeId: RecipeId,
  message: String = "Access to the recipe is forbidden",
)

final case class InternalServerError(
  message: String = "Something went wrong on the server side",
)

final case class InvalidInvitationHash(
  hash: String,
  message: String = "Invalid invitation hash",
)
