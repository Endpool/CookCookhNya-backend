package api.shoppinglist

import domain.UserId

import sttp.tapir.ztapir.*

val shoppingListEndpoint = endpoint
  .in("my" / "shopping-list")

val shoppingListEndpoints = List(
  addIngredients.widen,
  getIngredients.widen,
  deleteIngredients.widen,
)
