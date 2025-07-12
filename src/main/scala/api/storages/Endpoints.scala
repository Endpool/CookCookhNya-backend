package api.storages

import api.storages.ingredients.storagesIngredientsEndpoints
import api.storages.members.storagesMembersEndpoints

import sttp.tapir.ztapir.*

val storagesEndpoint =
  endpoint
    .tag("Storages")
    .in("my" / "storages")

val storageEndpoints = List(
  create.widen,
  delete.widen,
  getAll.widen,
  getSummary.widen,
) ++ storagesIngredientsEndpoints.map(_.widen)
  ++ storagesMembersEndpoints.map(_.widen)
