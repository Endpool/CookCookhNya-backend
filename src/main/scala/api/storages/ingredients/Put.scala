package api.storages.ingredients

import api.AppEnv
import api.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.IStorageIngredientsRepo
import domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val put: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .put
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(putHandler)

private def putHandler(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[IStorageIngredientsRepo, StorageError.NotFound | IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IStorageIngredientsRepo] {
    _.addIngredientToStorage(storageId, ingredientId)
  }
