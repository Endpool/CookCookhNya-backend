package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.StoragesRepo
import domain.{UserId, StorageId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetAllEnv = StoragesRepo

private val getAll: ZServerEndpoint[GetAllEnv, Any] =
  storagesEndpoint
  .get
  .out(jsonBody[Seq[StorageSummaryResp]])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(u : Unit):
  ZIO[AuthenticatedUser & GetAllEnv, InternalServerError, Seq[StorageSummaryResp]] =
  val userId = ???
  ZIO.serviceWithZIO[StoragesRepo](_.getAll(userId).map(_.map(StorageSummaryResp.fromDb))).mapError {
    _ => InternalServerError()
  }
