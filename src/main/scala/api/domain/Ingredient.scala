package api.domain

import api.domain.IngredientId

type IngredientId = BaseId

case class Ingredient(
                     ingredientId: IngredientId,
                     name: String
                     )

object Ingredient:
  final case class CreationEntity(name: String)
