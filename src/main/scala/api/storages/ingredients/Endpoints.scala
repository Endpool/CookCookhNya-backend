package api.storages.ingredients

import api.storages.storagesEndpoint
import domain.StorageId

import sttp.tapir.ztapir.*

val storagesIngredientsEndpoint =
  storagesEndpoint
  .in(path[StorageId]("storageId") / "ingredients")

val storagesIngredientsEndpoints = List(
  put.widen,
  getAll.widen,
  remove.widen,
  removeMany.widen
)
