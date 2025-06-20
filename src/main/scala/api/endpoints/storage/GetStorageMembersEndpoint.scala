package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageMembersRepoInterface
import api.domain.{StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.storageNotFoundVariant
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
ZIO[StorageMembersRepoInterface, StorageError.NotFound, Seq[UserId]] =
  ZIO.serviceWithZIO[StorageMembersRepoInterface] {
    _.getAllStorageMembers(storageId)
  }
