package api.ingredients.global

import sttp.tapir.ztapir.*

val globalIngredientsEndpoint =
  endpoint
    .in("ingredients")

val globalEndpoints = List(
  createGlobal.widen,
  getGlobal.widen,
  deleteGlobal.widen,
  searchGlobal.widen,
  searchForStorage.widen,
  searchForRecipe.widen
)
