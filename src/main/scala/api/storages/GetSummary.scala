package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.storages.checkForMembership
import db.DbError
import db.repositories.StorageMembersRepo
import db.repositories.StoragesRepo
import db.tables.DbStorage
import domain.{InternalServerError, StorageId, UserId}
import domain.StorageError.NotFound

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetSummaryEnv = StoragesRepo & StorageMembersRepo

private val getSummary: ZServerEndpoint[GetSummaryEnv, Any] =
  storagesEndpoint
  .get
  .in(path[StorageId]("storageId"))
  .out(jsonBody[StorageSummaryResp])
  .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getSummaryHandler)

private def getSummaryHandler(storageId: StorageId):
  ZIO[GetSummaryEnv, InternalServerError | NotFound, StorageSummaryResp] = {
  val userId = ???
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
