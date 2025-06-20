package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageIngredientsRepoInterface
import api.domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val addIngredientToStorageEndpoint: ZServerEndpoint[StorageIngredientsRepoInterface, Any] = myStoragesEndpoint
  .put
  .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(addIngredientToStorage)

private def addIngredientToStorage(userId: UserId):
((StorageId, IngredientId)) => ZIO[StorageIngredientsRepoInterface,
  StorageError.NotFound | IngredientError.NotFound,
  Unit] =
  case (storageId, ingredientId) =>
    ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
      _.addIngredientToStorage(storageId, ingredientId)
    }
