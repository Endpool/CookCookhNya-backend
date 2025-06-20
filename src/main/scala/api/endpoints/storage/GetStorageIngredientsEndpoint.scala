package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageIngredientsRepoInterface
import api.domain.{IngredientId, StorageError, StorageId, UserId}
import api.endpoints.GeneralEndpointData.{ingredientNotFoundVariant, storageNotFoundVariant}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getStorageIngredientsEndpoint: ZServerEndpoint[StorageIngredientsRepoInterface, Any] = myStoragesEndpoint
  .get
  .in(path[StorageId]("storageId") / "ingredients")
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[IngredientId]])
  .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getStorageIngredients)

private def getStorageIngredients(userId: UserId)(storageId: StorageId):
ZIO[StorageIngredientsRepoInterface, StorageError, Seq[IngredientId]] =
  ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
    _.getAllIngredientsFromStorage(storageId)
  }
