package api.storages

import api.AppEnv
import api.EndpointErrorVariants.storageNotFoundVariant
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val delete: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(deleteHandler)

private def deleteHandler(userId: UserId)(storageId: StorageId):
  ZIO[StoragesRepo, StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[StoragesRepo](_.removeById(storageId))
