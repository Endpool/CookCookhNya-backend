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

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageIngredientsRepo & IngredientsRepo & StoragesRepo & StorageMembersRepo,
      InternalServerError | NotFound,
      Seq[IngredientResp]] =
  val getAll = {
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
          missingEntry <- handleFailedSqlQuery(e)
          (keyName, keyValue, _) = missingEntry
          _ <- failIfStorageNotFound(keyName, keyValue)
        yield ()
      }: IO[InternalServerError | NotFound, Unit]
    }.flatMap(_ => ZIO.fail(InternalServerError()))
  }

  {
    for {
      storage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId)).flatMap {
        case Some(storage) => ZIO.succeed(storage)
        case None => ZIO.fail(NotFound(storageId.toString))
      }
      result <- ZIO.ifZIO(checkForMembership(userId, storage))(
        getAll, ZIO.fail(NotFound(storageId.toString))
      )
    } yield result
  }.mapError {
    case _: DbError => InternalServerError()
    case e: (InternalServerError | NotFound) => e
  }
