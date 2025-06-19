package domain

type IngredientId = BaseId

case class Ingredient(
                     ingredientId: IngredientId,
                     name: String
                     )
