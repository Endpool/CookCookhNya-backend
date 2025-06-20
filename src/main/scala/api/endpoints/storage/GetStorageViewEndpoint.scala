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

val getStorageViewEndpoint: ZServerEndpoint[StorageRepoInterface, Any] = myStoragesEndpoint
  .get
  .in(path[StorageId]("storageId"))
  .out(jsonBody[StorageView])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getStorageView)

private def getStorageView(userId: UserId)(storageId: StorageId):
ZIO[StorageRepoInterface, StorageError.NotFound, StorageView] =
  ZIO.serviceWithZIO[StorageRepoInterface] {
    _.getStorageViewById(storageId)
  }
