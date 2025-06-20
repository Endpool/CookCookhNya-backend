package api.ingredients

import api.AppEnv
import db.repositories.IIngredientsRepo
import domain.{IngredientError, IngredientId}
import api.GeneralEndpointData.ingredientNotFoundVariant

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
