package domain

case class Recipe(
  id: RecipeId,
  name: String,
  creatorId: UserId,
  ingredients: List[IngredientId],
  sourceLink: Option[String],
)
