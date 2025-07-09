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
import domain.{IngredientNotFound, StorageNotFound, IngredientId, InternalServerError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val removeMany: ZServerEndpoint[RemoveEnv, Any] =
  storagesIngredientsEndpoint
    .delete
    .in(query[Vector[IngredientId]]("ingredient"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(
      serverErrorVariant,
      ingredientNotFoundVariant,
      storageNotFoundVariant,
    ))
    .zSecuredServerLogic(removeManyHandler)

// TODO this endpoint ignored auth
private def removeManyHandler(storageId : StorageId, ingredientIds: Vector[IngredientId]):
ZIO[AuthenticatedUser & RemoveEnv,
  InternalServerError | StorageNotFound | IngredientNotFound,
  Unit] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] { repo =>
    ZIO.foreachDiscard(ingredientIds)(repo.removeIngredientFromStorageById(storageId, _))
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => toStorageNotFound(fkv) <|> toIngredientNotFound(fkv))
      .getOrElse(InternalServerError())
  }
