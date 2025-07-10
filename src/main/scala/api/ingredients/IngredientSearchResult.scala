package api.ingredients

import domain.IngredientId

final case class IngredientSearchResult(
   id: IngredientId,
   name: String,
   available: Boolean
)