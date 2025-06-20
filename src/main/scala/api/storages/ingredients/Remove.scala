package api.storages.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val remove: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(removeHandler)

private def removeHandler(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[StorageIngredientsRepo, StorageError.NotFound | IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.removeIngredientFromStorageById(storageId, ingredientId)
  }
