package api.ingredients

import api.ingredients.open.{createPublic, getPublic, deletePublic, searchPublic}
import api.ingredients.personal.{createPrivate, getPrivate, deletePrivate, searchPrivate}
import sttp.tapir.ztapir.*

val ingredientsEndpoints = List(
  createPublic.widen,
  createPrivate.widen,
  deletePublic.widen,
  deletePrivate.widen,
  getPublic.widen,
  getPrivate.widen,
  searchPublic.widen,
  searchPrivate.widen,
)
