package domain

case class Recipe(
  id: RecipeId,
  name: String,
  creatorId: UserId,
  isPublished: Boolean,
  ingredients: List[IngredientId],
  sourceLink: Option[String],
)
