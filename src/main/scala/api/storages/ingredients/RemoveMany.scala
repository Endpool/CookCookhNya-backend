package api.storages.ingredients

import api.{
  AppEnv,
  zSecuredServerLogic,
  handleFailedSqlQuery,
  toIngredientNotFound,
  toStorageNotFound,
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

private val removeMany: ZServerEndpoint[AppEnv, Any] =
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

private def removeManyHandler(userId: UserId)(storageId : StorageId, ingredientIds: Vector[IngredientId]):
ZIO[StorageIngredientsRepo,
  InternalServerError | StorageError.NotFound | IngredientError.NotFound,
  Unit] = {
  ZIO.serviceWithZIO[StorageIngredientsRepo] { repo => 
    ZIO.foreachDiscard(ingredientIds)(repo.removeIngredientFromStorageById(storageId, _))
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => toStorageNotFound(fkv) <|> toIngredientNotFound(fkv))
      .getOrElse(InternalServerError())
  }
}
