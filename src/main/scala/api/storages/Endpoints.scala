package api.storages

import api.storages.ingredients.storagesIngredientsEndpoints
import api.storages.members.storagesMembersEndpoints
import domain.UserId

import sttp.tapir.ztapir.*

val storagesEndpoint =
  endpoint
  .in("my" / "storages")
  .securityIn(auth.bearer[UserId]())

val storageEndpoints = List(
  create.widen,
  delete.widen,
  getAll.widen,
  getSummary.widen,
) ++ storagesIngredientsEndpoints.map(_.widen)
  ++ storagesMembersEndpoints.map(_.widen)
