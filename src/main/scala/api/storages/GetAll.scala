package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{UserId, StorageId, InternalServerError}
import api.zSecuredServerLogic
import api.EndpointErrorVariants.serverErrorVariant

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .out(jsonBody[Seq[StorageSummaryResp]])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(u : Unit):
  ZIO[StoragesRepo, InternalServerError, Seq[StorageSummaryResp]] =
  ZIO.serviceWithZIO[StoragesRepo](_.getAll(userId).map(_.map(StorageSummaryResp.fromDb))).mapError {
    _ => InternalServerError()
  }
