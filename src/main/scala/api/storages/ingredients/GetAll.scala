package api.storages.ingredients

import api.{
  handleFailedSqlQuery,
  toStorageNotFound,
  toUserNotFound,
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant,
  userNotFoundVariant
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.ingredients.IngredientResp
import db.repositories.{IngredientsRepo, StorageIngredientsRepo, StorageMembersRepo, StoragesRepo}
import db.DbError.{FailedDbQuery, DbNotRespondingError}
import domain.{StorageNotFound, InternalServerError, UserNotFound, IngredientId, StorageId, UserId}
import common.OptionExtensions.<|>

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{ZIO, IO}

private type GetAllEnv = StorageIngredientsRepo & IngredientsRepo & StoragesRepo & StorageMembersRepo

private val getAll: ZServerEndpoint[GetAllEnv, Any] =
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

private def getAllHandler(storageId: StorageId):
  ZIO[AuthenticatedUser & GetAllEnv,
      InternalServerError | StorageNotFound | UserNotFound,
      Seq[IngredientResp]] = {
    for
      storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
        .someOrFail(StorageNotFound(storageId.toString))
      ingredientIds <- ZIO.serviceWithZIO[StorageIngredientsRepo](_
        .getAllIngredientsFromStorage(storageId)
      )
      ingredients <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
        ZIO.foreach(ingredientIds)(repo.getPersonal)
      )
    yield ingredients.flatten.map(IngredientResp.fromDb)
  }.mapError {
    case _: (DbNotRespondingError | InternalServerError) => InternalServerError()
    case e: StorageNotFound => e
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(fkv => toStorageNotFound(fkv) <|> toUserNotFound(fkv))
      .getOrElse(InternalServerError())
  }
