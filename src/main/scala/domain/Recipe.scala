package domain

case class Recipe(
  id: RecipeId,
  name: String,
  creatorId: Option[UserId],
  isPublished: Boolean,
  ingredients: List[IngredientId],
  sourceLink: Option[String],
)
