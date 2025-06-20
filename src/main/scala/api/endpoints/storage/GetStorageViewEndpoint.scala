package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.IStoragesRepo
import api.domain.{StorageError, StorageId, StorageView, UserId}
import api.endpoints.GeneralEndpointData.storageNotFoundVariant
import api.AppEnv

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getStorageViewEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .get
  .in(path[StorageId]("storageId"))
  .out(jsonBody[StorageView])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getStorageView)

private def getStorageView(userId: UserId)(storageId: StorageId):
ZIO[IStoragesRepo, StorageError.NotFound, StorageView] =
  ZIO.serviceWithZIO[IStoragesRepo] {
    _.getStorageViewById(storageId)
  }
