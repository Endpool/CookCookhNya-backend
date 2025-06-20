package domain

import domain.IngredientId

type IngredientId = BaseId

case class Ingredient(
                     ingredientId: IngredientId,
                     name: String
                     )
