package api.ingredients

import sttp.tapir.ztapir.*

val ingredientsEndpoint =
  endpoint
  .in("ingredients")

val ingredientsEndpoints = List(
  create,
  delete,
  getAll,
  get,
)
