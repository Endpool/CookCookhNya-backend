package api.storages.ingredients

import api.{
  AppEnv,
  handleFailedSqlQuery,
  failIfStorageNotFound,
  failIfUserNotFound
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant,
  userNotFoundVariant
}
import api.storages.checkForMembership
import api.zSecuredServerLogic
import api.ingredients.IngredientResp
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo, StoragesRepo}
import domain.{InternalServerError, UserError, IngredientId, StorageId, UserId}
import domain.StorageError.NotFound
import db.DbError.{FailedDbQuery, DbNotRespondingError}
import db.DbError

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

type Env = StorageIngredientsRepo & IngredientsRepo & StoragesRepo & StorageMembersRepo
private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[Env, InternalServerError | NotFound, Seq[IngredientResp]] = {
  for
    storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .someOrFail(NotFound(storageId.toString))
    _ <- ZIO.unlessZIO[Env, NotFound | InternalServerError](checkForMembership(userId, storage)) {
      ZIO.fail(NotFound(storageId.toString))
    }
    ingredientIds <- ZIO.serviceWithZIO[StorageIngredientsRepo] {
      _.getAllIngredientsFromStorage(storageId)
    }
    ingredients <- ZIO.serviceWithZIO[IngredientsRepo] { repo =>
      ZIO.foreach(ingredientIds) { repo.getById(_) }
    }
  yield ingredients.flatten.map(IngredientResp.fromDb)
}.mapError {
  case _: (DbNotRespondingError | InternalServerError) => InternalServerError()
  case e: NotFound => e
  case e: FailedDbQuery => handleFailedSqlQuery(e)
    .flatMap(failIfStorageNotFound)
    .getOrElse(InternalServerError())
}

