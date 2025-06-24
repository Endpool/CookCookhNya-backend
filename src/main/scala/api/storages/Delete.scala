package api.storages

import api.AppEnv
import api.EndpointErrorVariants.{
  databaseFailureErrorVariant,
  serverUnexpectedErrorVariant,
  storageNotFoundVariant
}
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{StorageId, UserId}
import domain.DbError.{DbNotRespondingError, UnexpectedDbError}
import domain.StorageError.NotFound

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val delete: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverUnexpectedErrorVariant, databaseFailureErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(deleteHandler)

private def deleteHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, UnexpectedDbError | DbNotRespondingError | NotFound, Unit] =
  ZIO.serviceWithZIO[StoragesRepo](_.removeById(storageId)).mapError {
    error => ???
  }
