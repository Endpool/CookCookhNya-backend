package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{StorageError, StorageId, UserId}
import api.EndpointErrorVariants.storageNotFoundVariant
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
  .out(jsonBody[StorageSummaryResp])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getSummaryHandler)

private def getSummaryHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, StorageError.NotFound, StorageSummaryResp] = for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .catchAll { e => ??? } // TODO handle error
    storage  <- ZIO.fromOption(mStorage)
      .orElseFail[StorageError.NotFound](StorageError.NotFound(storageId))
  yield StorageSummaryResp.fromDb(storage)
