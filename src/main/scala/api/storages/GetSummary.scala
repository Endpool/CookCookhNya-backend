package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import db.repositories.StorageMembersRepo
import domain.{InternalServerError, StorageId, UserId}
import domain.StorageError.NotFound
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.DbError
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
  .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getSummaryHandler)

private def getSummaryHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo & StorageMembersRepo, InternalServerError | NotFound, StorageSummaryResp] =
  {
    for
      mStorage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      storage <- ZIO.fromOption(mStorage)
        .orElseFail(NotFound(storageId))
      _ <- checkForMembership(userId, storage)
    yield StorageSummaryResp.fromDb(storage)
  }.mapError {
    case e: NotFound => e
    case _ => InternalServerError()
  }

def checkForMembership(userId: UserId, storage: DbStorage):
  ZIO[StorageMembersRepo, DbError | NotFound, Unit] =
  if userId == storage.ownerId then ZIO.unit
  else
    ZIO.serviceWithZIO[StorageMembersRepo] {
      _.getAllStorageMembers(storage.id)
    }.flatMap {
      members =>
        if members.contains(userId) then ZIO.unit
        else ZIO.fail(NotFound(storage.id))
    }
