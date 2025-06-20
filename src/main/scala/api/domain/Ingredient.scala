package api.domain

import api.domain.IngredientId

type IngredientId = BaseId

case class Ingredient(
                     ingredientId: IngredientId,
                     name: String
                     )
