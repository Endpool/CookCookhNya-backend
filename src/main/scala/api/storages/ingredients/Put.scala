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
import domain.DbError.{DbNotRespondingError, FailedDbQuery, UnexpectedDbError}
import db.getAbsentKey

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
    _.addIngredientToStorage(storageId, ingredientId).mapError {
      case error: (UnexpectedDbError | DbNotRespondingError) => error
      case FailedDbQuery(exc) =>
        getAbsentKey(exc.getServerErrorMessage.getDetail) match
          case Some(key) =>
            if key == "storage_id" then StorageError.NotFound(storageId) // TODO: change key validation
            else if key == "ingredient_id" then IngredientError.NotFound(ingredientId)
            else UnexpectedDbError(exc.getMessage)
          case None => UnexpectedDbError(exc.getMessage)
    }
  }
