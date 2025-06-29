package api.storages

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{StorageId, UserId, InternalServerError}
import db.DbError

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val delete: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(deleteHandler)

private def deleteHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, InternalServerError, Unit] =
  ZIO.serviceWithZIO[StoragesRepo](_.removeById(storageId)).mapError {
    _ => InternalServerError()
  }
