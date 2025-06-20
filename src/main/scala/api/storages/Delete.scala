package api.storages

import api.AppEnv
import api.GeneralEndpointData.storageNotFoundVariant
import api.zSecuredServerLogic
import db.repositories.IStoragesRepo
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
  ZIO[IStoragesRepo, StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[IStoragesRepo](_.removeById(storageId))
