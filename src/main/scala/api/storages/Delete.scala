package api.storages

import api.EndpointErrorVariants.serverErrorVariant
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import db.repositories.StoragesRepo
import domain.{StorageId, UserId, InternalServerError}
import db.DbError

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteEnv = StoragesRepo

private val delete: ZServerEndpoint[DeleteEnv, Any] =
  storagesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(deleteHandler)

private def deleteHandler(storageId: StorageId):
  ZIO[AuthenticatedUser & DeleteEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[StoragesRepo](_.removeById(storageId)).mapError {
    _ => InternalServerError()
  }
