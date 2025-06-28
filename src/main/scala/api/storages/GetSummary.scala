package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import db.repositories.StorageMembersRepo
import domain.{StorageError, StorageId, UserId}
import api.EndpointErrorVariants.storageNotFoundVariant
import api.zSecuredServerLogic
import db.tables.DbStorage

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
  ZIO[StoragesRepo & StorageMembersRepo, StorageError.NotFound, StorageSummaryResp] =
  for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .catchAll { e => ??? } // TODO handle error
    storage <- ZIO.fromOption(mStorage)
      .orElseFail[StorageError.NotFound](StorageError.NotFound(storageId))
    _ <- checkForMembership(userId, storage)
  yield StorageSummaryResp.fromDb(storage)

def checkForMembership(userId: UserId, storage: DbStorage):
  ZIO[StorageMembersRepo, StorageError.NotFound, Unit] =
  if userId == storage.ownerId then ZIO.unit
  else
    ZIO.serviceWithZIO[StorageMembersRepo] {
      _.getAllStorageMembers(storage.id)
    }.catchAll(_ => ???).flatMap {
      members =>
        if members.contains(userId) then ZIO.unit
        else ZIO.fail(StorageError.NotFound(storage.id))
    }
