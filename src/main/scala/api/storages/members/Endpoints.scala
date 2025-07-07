package api.storages.members

import api.storages.storagesEndpoint
import domain.StorageId

import sttp.tapir.ztapir.*

val storagesMembersEndpoint =
  storagesEndpoint
  .in(path[StorageId]("storageId") / "members")

val storagesMembersEndpoints = List(
  getAll.widen,
  add.widen,
  remove.widen,
)
