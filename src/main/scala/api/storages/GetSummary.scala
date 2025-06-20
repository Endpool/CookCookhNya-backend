package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{StorageError, StorageId, StorageView, UserId}
import api.GeneralEndpointData.storageNotFoundVariant
import api.zSecuredServerLogic

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getSummary: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .in(path[StorageId]("storageId"))
  .out(jsonBody[StorageView])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getStorageView)

private def getStorageView(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, StorageError.NotFound, StorageView] =
  ZIO.serviceWithZIO[StoragesRepo] {
    _.getStorageViewById(storageId)
  }
