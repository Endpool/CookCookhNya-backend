package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{StorageId, UserId}
import domain.StorageError.NotFound
import domain.DbError.{UnexpectedDbError, DbNotRespondingError}
import api.EndpointErrorVariants.{
  storageNotFoundVariant,
  serverUnexpectedErrorVariant,
  databaseFailureErrorVariant
}
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
  .errorOut(oneOf(serverUnexpectedErrorVariant, databaseFailureErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getSummaryHandler)

private def getSummaryHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, UnexpectedDbError | DbNotRespondingError | NotFound, StorageSummaryResp] = for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .catchAll { e => ??? } // TODO handle error
    storage  <- ZIO.fromOption(mStorage)
      .orElseFail[NotFound](NotFound(storageId))
  yield dbToResp(storage)
