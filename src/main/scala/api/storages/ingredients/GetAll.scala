package api.storages.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{serverErrorVariant}
import api.zSecuredServerLogic
import api.ingredients.IngredientResp
import db.repositories.StorageIngredientsRepo
import domain.{IngredientId, StorageError, DbError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import db.repositories.IngredientsRepo

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesIngredientsEndpoint
  .get
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[IngredientResp]])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageIngredientsRepo & IngredientsRepo, DbError.UnexpectedDbError, Seq[IngredientResp]] = for
    ingredientIds <- ZIO.serviceWithZIO[StorageIngredientsRepo] {
      _.getAllIngredientsFromStorage(storageId)
    }
    ingredients <- ZIO.foreach(ingredientIds){ ingredientId =>
      ZIO.serviceWithZIO[IngredientsRepo] {
        _.getById(ingredientId)
      }
    }
  yield ingredients.flatten.map(IngredientResp.fromDb)
