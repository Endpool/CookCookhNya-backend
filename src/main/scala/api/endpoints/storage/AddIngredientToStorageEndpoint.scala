package api.endpoints.storage

import api.endpoints.zSecuredServerLogic
import api.db.repositories.IStorageIngredientsRepo
import api.domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.AppEnv

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val addIngredientToStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .put
  .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(addIngredientToStorage)

private def addIngredientToStorage(userId: UserId):
((StorageId, IngredientId)) => ZIO[IStorageIngredientsRepo,
  StorageError.NotFound | IngredientError.NotFound,
  Unit] =
  case (storageId, ingredientId) =>
    ZIO.serviceWithZIO[IStorageIngredientsRepo] {
      _.addIngredientToStorage(storageId, ingredientId)
    }
