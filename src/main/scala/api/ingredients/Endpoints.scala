package api.ingredients

import sttp.tapir.ztapir.*

val ingredientsEndpoint =
  endpoint
  .in("ingredients")

val ingredientsEndpoints = List(
  create.widen,
  delete.widen,
  get.widen,
  search.widen,
  searchAll.widen,
)
