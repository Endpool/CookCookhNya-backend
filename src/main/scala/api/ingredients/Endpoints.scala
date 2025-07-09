package api.ingredients

import sttp.tapir.ztapir.*

val ingredientsEndpoint =
  endpoint
  .in("ingredients")

val ingredientsEndpoints = List(
  createPublic.widen,
  createPrivate.widen,
  delete.widen,
  get.widen,
  search.widen,
  searchAll.widen,
)
