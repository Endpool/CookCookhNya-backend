package api.storages.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{
  databaseFailureErrorVariant,
  ingredientNotFoundVariant,
  serverUnexpectedErrorVariant,
  storageNotFoundVariant
}
import api.zSecuredServerLogic
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, StorageError, StorageId, UserId}
import domain.DbError.{UnexpectedDbError, DbNotRespondingError}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val put: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .put
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(
    serverUnexpectedErrorVariant,
    databaseFailureErrorVariant,
    ingredientNotFoundVariant,
    storageNotFoundVariant,
  ))
  .zSecuredServerLogic(putHandler)

private def putHandler(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[StorageIngredientsRepo,
     UnexpectedDbError | DbNotRespondingError | IngredientError.NotFound | StorageError.NotFound,
     Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.addIngredientToStorage(storageId, ingredientId)
  }
