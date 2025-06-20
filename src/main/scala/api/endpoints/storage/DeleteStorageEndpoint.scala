package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageRepoInterface
import api.domain.{StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.storageNotFoundVariant
import api.AppEnv

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val deleteStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(deleteStorage)

private def deleteStorage(userId: UserId)(storageId: StorageId):
ZIO[StorageRepoInterface, StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[StorageRepoInterface](_.removeById(storageId))
