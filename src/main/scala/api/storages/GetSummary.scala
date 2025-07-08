package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.storages.checkForMembership
import db.DbError
import db.repositories.StorageMembersRepo
import db.repositories.StoragesRepo
import db.tables.DbStorage
import domain.{StorageNotFound, InternalServerError, StorageId, UserId}

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
      InternalServerError | StorageNotFound,
      StorageSummaryResp] =
  ZIO.serviceWithZIO[StoragesRepo](_
    .getById(storageId)
    .someOrFail(StorageNotFound(storageId.toString))
    .map(StorageSummaryResp.fromDb)
    .mapError {
      case e: StorageNotFound => e
      case _ => InternalServerError()
    }
  )
