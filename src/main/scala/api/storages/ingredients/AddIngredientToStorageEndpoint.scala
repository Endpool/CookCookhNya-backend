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

val addIngredientToStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .put
  .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(addIngredientToStorage)

private def addIngredientToStorage(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[IStorageIngredientsRepo, StorageError.NotFound | IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IStorageIngredientsRepo] {
    _.addIngredientToStorage(storageId, ingredientId)
  }
