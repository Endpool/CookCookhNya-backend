package api.storages.ingredients

import api.storages.storagesEndpoint
import api.TapirExtensions.subTag
import domain.StorageId

import sttp.tapir.ztapir.*

val storagesIngredientsEndpoint =
  storagesEndpoint
    .subTag("Ingredients")
    .in(path[StorageId]("storageId") / "ingredients")

val storagesIngredientsEndpoints = List(
  put.widen,
  getAll.widen,
  remove.widen,
  removeMany.widen
)
