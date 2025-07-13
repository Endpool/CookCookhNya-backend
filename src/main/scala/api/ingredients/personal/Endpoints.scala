package api.ingredients.personal

import api.ingredients.ingredientsEndpoint

import sttp.tapir.ztapir.*

private val personalIngredientsEndpoint =
  ingredientsEndpoint

val personalEndpoints = List(
  createPersonal.widen,
  getPersonal.widen,
  deletePersonal.widen,
  searchPersonal.widen
)
