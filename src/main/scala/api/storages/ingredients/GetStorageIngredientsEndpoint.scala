package api.storages.ingredients

import api.AppEnv
import api.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.IStorageIngredientsRepo
import domain.{IngredientId, StorageError, StorageId, UserId}

import sttp.model.StatusCode
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

import api.storages.myStoragesEndpoint

val getStorageIngredientsEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .get
  .in(path[StorageId]("storageId") / "ingredients")
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[IngredientId]])
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getStorageIngredients)

private def getStorageIngredients(userId: UserId)(storageId: StorageId):
  ZIO[IStorageIngredientsRepo, StorageError, Seq[IngredientId]] =
  ZIO.serviceWithZIO[IStorageIngredientsRepo] {
    _.getAllIngredientsFromStorage(storageId)
  }
