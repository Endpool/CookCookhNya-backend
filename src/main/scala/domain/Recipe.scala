package domain

type RecipeId = BaseId

case class Recipe(
                 id: RecipeId,
                 name: String,
                 ingredients: List[IngredientId],
                 sourceLink: String
                 )