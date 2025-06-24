package domain

import domain.IngredientId

type IngredientId = BaseId

case class Ingredient(
  id: IngredientId,
  name: String
)
