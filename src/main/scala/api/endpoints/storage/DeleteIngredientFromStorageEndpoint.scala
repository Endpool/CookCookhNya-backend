package api.endpoints.storage

import api.endpoints.zSecuredServerLogic
import api.db.repositories.IStorageIngredientsRepo
import api.domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.AppEnv

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

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
