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
  create,
  delete,
  getAll,
  getSummary,
) ++ storagesIngredientsEndpoints
  ++ storagesMembersEndpoints
