package api.shoppinglist

import sttp.tapir.ztapir.*

val shoppingListEndpoint =
  endpoint
    .tag("Shopping lists")
    .in("shopping-list")

val shoppingListEndpoints = List(
  addIngredients.widen,
  getIngredients.widen,
  deleteIngredients.widen,
  buy.widen
)
