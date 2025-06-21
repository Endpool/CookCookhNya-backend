package api.storages.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.StorageIngredientsRepo
import domain.{IngredientId, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .get
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[IngredientId]])
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageIngredientsRepo, StorageError, Seq[IngredientId]] =
  ZIO.serviceWithZIO[StorageIngredientsRepo] {
    _.getAllIngredientsFromStorage(storageId)
  }
