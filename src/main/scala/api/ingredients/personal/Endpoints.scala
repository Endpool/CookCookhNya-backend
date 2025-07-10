package api.ingredients.personal

import sttp.tapir.ztapir.*

private val personalIngredientsEndpoint =
  endpoint
    .in("my" / "ingredients")

val personalEndpoints = List(
  createPersonal.widen,
  getPersonal.widen,
  deletePersonal.widen,
  searchPersonal.widen
)
