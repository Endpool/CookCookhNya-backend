package api.ingredients.global

import sttp.tapir.ztapir.*

val globalEndpoints = List(
  createGlobal.widen,
  getGlobal.widen,
  deleteGlobal.widen,
  searchGlobal.widen
)
