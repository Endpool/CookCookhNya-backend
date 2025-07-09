package domain


case class Recipe(
  id: RecipeId,
  name: String,
  ingredients: Vector[IngredientId],
  sourceLink: String
)
