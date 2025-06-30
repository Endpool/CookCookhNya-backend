package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import db.repositories.StorageMembersRepo
import domain.{InternalServerError, StorageId, UserId}
import domain.StorageError.NotFound
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import api.storages.checkForMembership
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
        .orElseFail(NotFound(storageId.toString))
      _ <- checkForMembership(userId, storage)
      result <- ZIO.ifZIO(checkForMembership(userId, storage))(
        ZIO.succeed(StorageSummaryResp.fromDb(storage)),
        ZIO.fail(NotFound(storageId.toString))
      )
    yield result
  }.mapError {
    case e: NotFound => e
    case _ => InternalServerError()
  }
