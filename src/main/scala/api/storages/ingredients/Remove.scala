package api.storages.ingredients

import api.{
  handleFailedSqlQuery,
  toIngredientNotFound,
  toStorageNotFound,
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
import domain.{IngredientNotFound, StorageNotFound, IngredientId, InternalServerError, StorageId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type RemoveEnv = StorageIngredientsRepo

private val remove: ZServerEndpoint[RemoveEnv, Any] =
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

// TODO this endpoint ignored auth
private def removeHandler(storageId : StorageId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & RemoveEnv,
      InternalServerError | StorageNotFound | IngredientNotFound,
      Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.removeIngredientFromStorageById(storageId, ingredientId)
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => toStorageNotFound(fkv) <|> toIngredientNotFound(fkv))
      .getOrElse(InternalServerError())
  }
