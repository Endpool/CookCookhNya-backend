package api.storages

import api.AppEnv
import api.{handleFailedSqlQuery, toUserNotFound}
import db.repositories.StoragesRepo
import db.DbError
import domain.{UserId, StorageId, InternalServerError}
import domain.UserError.NotFound
import api.zSecuredServerLogic
import api.EndpointErrorVariants.{serverErrorVariant, userNotFoundVariant}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .out(jsonBody[Seq[StorageSummaryResp]])
  .errorOut(oneOf(serverErrorVariant, userNotFoundVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(u : Unit):
  ZIO[StoragesRepo, InternalServerError | NotFound, Seq[StorageSummaryResp]] =
  ZIO.serviceWithZIO[StoragesRepo](_.getAll(userId).map(_.map(StorageSummaryResp.fromDb))).catchAll {
    case DbError.DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: DbError.FailedDbQuery => handleFailedSqlQuery(e).flatMap {
      toUserNotFound(_, userId).flatMap(_ => ZIO.fail(InternalServerError()))
    }
  }
