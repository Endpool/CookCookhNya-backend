package api.storages.ingredients

import api.{
  AppEnv,
  handleFailedSqlQuery,
  toStorageNotFound,
  toUserNotFound
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant,
  userNotFoundVariant
}
import api.zSecuredServerLogic
import api.ingredients.IngredientResp
import db.repositories.StorageIngredientsRepo
import domain.{InternalServerError, UserError, IngredientId, StorageError, StorageId, UserId}
import db.DbError.{FailedDbQuery, DbNotRespondingError}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{ZIO, IO}
import db.repositories.IngredientsRepo

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .get
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[IngredientResp]])
  .errorOut(oneOf(
    serverErrorVariant,
    userNotFoundVariant,
    storageNotFoundVariant
  ))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageIngredientsRepo & IngredientsRepo,
      InternalServerError | StorageError.NotFound | UserError.NotFound,
      Seq[IngredientResp]] =
  {
    for
      ingredientIds <- ZIO.serviceWithZIO[StorageIngredientsRepo] {
        _.getAllIngredientsFromStorage(storageId)
      }
      ingredients <- ZIO.foreach(ingredientIds) { ingredientId =>
        ZIO.serviceWithZIO[IngredientsRepo] {
          _.getById(ingredientId)
        }
      }
    yield ingredients.flatten.map(IngredientResp.fromDb)
  }.catchAll {
    case DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: FailedDbQuery => {
      {
        for
          keyName <- handleFailedSqlQuery(e)
          _ <- toStorageNotFound(keyName, storageId)
          _ <- toUserNotFound(keyName, userId)
        yield ()
      }: IO[InternalServerError | StorageError.NotFound | UserError.NotFound, Unit]
    }.flatMap(_ => ZIO.fail(InternalServerError()))
  }
