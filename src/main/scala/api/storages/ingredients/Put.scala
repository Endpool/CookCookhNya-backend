package api.storages.ingredients

import api.{
  handleFailedSqlQuery,
  toStorageNotFound,
  toIngredientNotFound,
}
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import common.OptionExtensions.<|>
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, InternalServerError, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type PutEnv = StorageIngredientsRepo

private val put: ZServerEndpoint[PutEnv, Any] =
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

// TODO this endpoint ignored auth
private def putHandler(storageId : StorageId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & PutEnv,
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

