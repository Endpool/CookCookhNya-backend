package api.storages.members

import api.storages.storagesEndpoint
import api.TapirExtensions.subTag
import domain.StorageId

import sttp.tapir.ztapir.*

val storagesMembersEndpoint =
  storagesEndpoint
    .subTag("Members")
    .in(path[StorageId]("storageId") / "members")

val storagesMembersEndpoints = List(
  getAll.widen,
  add.widen,
  remove.widen,
)
