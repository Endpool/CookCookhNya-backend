package api.storages.ingredients

import api.{
  AppEnv,
  handleFailedSqlQuery,
  toIngredientNotFound,
  toStorageNotFound
}
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.StorageIngredientsRepo
import domain.{IngredientError, IngredientId, InternalServerError, StorageError, StorageId, UserId}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
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
  }.catchAll {
    case DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: FailedDbQuery => for {
        keyName <- handleFailedSqlQuery(e)
        _ <- toStorageNotFound(keyName, storageId)
        _ <- toIngredientNotFound(keyName, ingredientId)
        _ <- ZIO.fail(InternalServerError())
      } yield ()
  }
