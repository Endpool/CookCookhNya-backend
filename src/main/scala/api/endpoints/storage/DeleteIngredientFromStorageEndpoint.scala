package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageIngredientsRepoInterface
import api.domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val deleteIngredientFromStorageEndpoint: ZServerEndpoint[StorageIngredientsRepoInterface, Any] = myStoragesEndpoint
  .delete
  .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(deleteMyIngredientFromStorage)

private def deleteMyIngredientFromStorage(userId: UserId):
  ((StorageId, IngredientId)) => ZIO[StorageIngredientsRepoInterface,
                                     StorageError.NotFound | IngredientError.NotFound,
                                     Unit] =
  case (storageId, ingredientId) =>
    ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
      _.removeIngredientFromStorageById(storageId, ingredientId)
    }
