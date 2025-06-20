package api.storages

import api.AppEnv
import db.repositories.IStoragesRepo
import domain.{StorageError, StorageId, UserId}
import api.GeneralEndpointData.storageNotFoundVariant
import api.zSecuredServerLogic

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val deleteStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(deleteStorage)

private def deleteStorage(userId: UserId)(storageId: StorageId):
  ZIO[IStoragesRepo, StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[IStoragesRepo](_.removeById(storageId))
