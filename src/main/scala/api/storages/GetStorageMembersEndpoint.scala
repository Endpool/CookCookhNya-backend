package api.storages

import api.zSecuredServerLogic
import db.repositories.IStorageMembersRepo
import domain.{StorageError, StorageId, UserId}
import api.GeneralEndpointData.storageNotFoundVariant
import api.AppEnv

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getStorageMembersEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .get
  .in(path[StorageId]("storageId") / "members")
  .out(jsonBody[Seq[UserId]])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getStorageMembers)

private def getStorageMembers(userId: UserId)(storageId: StorageId):
ZIO[IStorageMembersRepo, StorageError.NotFound, Seq[UserId]] =
  ZIO.serviceWithZIO[IStorageMembersRepo] {
    _.getAllStorageMembers(storageId)
  }
