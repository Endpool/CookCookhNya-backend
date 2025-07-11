package domain


case class Recipe(
  id: RecipeId,
  name: String,
  ingredients: List[IngredientId],
  sourceLink: String
)
