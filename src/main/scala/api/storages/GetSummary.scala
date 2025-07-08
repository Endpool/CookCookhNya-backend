package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.storages.checkForMembership
import db.DbError
import db.repositories.StorageMembersRepo
import db.repositories.StoragesRepo
import db.tables.DbStorage
import domain.{StorageError, InternalServerError, StorageId, UserId}

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
  ZIO[AuthenticatedUser & GetSummaryEnv,
      InternalServerError | StorageError.NotFound,
      StorageSummaryResp] =
  ZIO.serviceWithZIO[StoragesRepo](_
    .getById(storageId)
    .someOrFail(StorageError.NotFound(storageId.toString))
    .map(StorageSummaryResp.fromDb)
    .mapError {
      case e: StorageError.NotFound => e
      case _ => InternalServerError()
    }
  )
