package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageRepoInterface
import api.domain.{Storage, StorageError, StorageId, StorageView, UserId}
import api.endpoints.GeneralEndpointData.storageNotFoundVariant

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

val deleteStorageEndpoint: ZServerEndpoint[StorageRepoInterface, Any] = myStoragesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(deleteStorage)

private def deleteStorage(userId: UserId)(storageId: StorageId):
ZIO[StorageRepoInterface, StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[StorageRepoInterface](_.removeById(storageId))
