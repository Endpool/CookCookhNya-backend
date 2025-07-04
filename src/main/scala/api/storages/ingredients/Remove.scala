package api.storages.ingredients

import api.{
  AppEnv,
  zSecuredServerLogic,
  handleFailedSqlQuery,
  failIfIngredientNotFound,
  failIfStorageNotFound,
}
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant
}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, InternalServerError, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val remove: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(
    serverErrorVariant,
    ingredientNotFoundVariant,
    storageNotFoundVariant,
  ))
  .zSecuredServerLogic(removeHandler)

private def removeHandler(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[StorageIngredientsRepo,
      InternalServerError | StorageError.NotFound | IngredientError.NotFound,
      Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.removeIngredientFromStorageById(storageId, ingredientId)
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => failIfStorageNotFound(fkv) <|> failIfIngredientNotFound(fkv))
      .getOrElse(InternalServerError())
  }
