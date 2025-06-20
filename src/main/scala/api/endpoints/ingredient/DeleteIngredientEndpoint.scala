package api.endpoints.ingredient

import api.AppEnv
import api.db.repositories.IIngredientsRepo
import api.domain.{IngredientError, IngredientId}
import api.endpoints.GeneralEndpointData.ingredientNotFoundVariant

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
  ZIO[IIngredientsRepo, IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IIngredientsRepo] {
    _.removeById(ingredientId)
  }
