package api.storages.ingredients

import api.AppEnv
import api.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.IStorageIngredientsRepo
import domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

import api.storages.myStoragesEndpoint

val deleteIngredientFromStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .delete
  .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(deleteMyIngredientFromStorage)

private def deleteMyIngredientFromStorage(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[IStorageIngredientsRepo, StorageError.NotFound | IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IStorageIngredientsRepo] {
    _.removeIngredientFromStorageById(storageId, ingredientId)
  }
