package api.storages.ingredients

import api.{
  AppEnv,
  handleFailedSqlQuery,
  zSecuredServerLogic,
  toStorageNotFound,
  toIngredientNotFound,
}
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant
}
import common.OptionExtensions.<|>
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, InternalServerError, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

val put: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .put
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(
    serverErrorVariant,
    ingredientNotFoundVariant,
    storageNotFoundVariant,
  ))
  .zSecuredServerLogic(putHandler)

private def putHandler(userId: UserId)(storageId : StorageId, ingredientId: IngredientId):
  ZIO[StorageIngredientsRepo,
      InternalServerError | IngredientError.NotFound | StorageError.NotFound,
      Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.addIngredientToStorage(storageId, ingredientId)
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => toStorageNotFound(fkv) <|> toIngredientNotFound(fkv))
      .getOrElse(InternalServerError())
  }

