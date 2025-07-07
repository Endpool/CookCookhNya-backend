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
import api.storages.checkForMembership
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
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
  ZIO[AuthenticatedUser & GetAllEnv, InternalServerError | NotFound, Seq[IngredientResp]] = {
  for
    storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .someOrFail(NotFound(storageId.toString))
    _ <- ZIO.unlessZIO[AuthenticatedUser & GetAllEnv, NotFound | InternalServerError]
      (checkForMembership(storage))(
        ZIO.fail(NotFound(storageId.toString))
      )
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
    .flatMap(toStorageNotFound)
    .getOrElse(InternalServerError())
}

