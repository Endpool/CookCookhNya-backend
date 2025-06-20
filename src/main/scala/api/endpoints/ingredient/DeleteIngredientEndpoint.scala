package api.endpoints.ingredient

import api.db.repositories.IngredientRepoInterface
import api.domain.{IngredientError, IngredientId}
import api.endpoints.GeneralEndpointData.ingredientNotFoundVariant
import api.AppEnv

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val deleteIngredientEndpoint: ZServerEndpoint[AppEnv, Any] = endpoint
  .delete
  .in("ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant))
  .zServerLogic(deleteIngredient)

def deleteIngredient(ingredientId: IngredientId):
ZIO[IngredientRepoInterface, IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IngredientRepoInterface] {
    _.removeById(ingredientId)
  }
