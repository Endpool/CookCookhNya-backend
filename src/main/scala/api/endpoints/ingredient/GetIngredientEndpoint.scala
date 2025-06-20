package api.endpoints.ingredient

import api.db.repositories.IIngredientRepo
import api.domain.{Ingredient, IngredientError, IngredientId}
import api.endpoints.GeneralEndpointData.ingredientNotFoundVariant
import api.AppEnv

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getIngredientEndpoint: ZServerEndpoint[AppEnv, Any] = endpoint
  .get
  .in("ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Ingredient])
  .errorOut(oneOf(ingredientNotFoundVariant))
  .zServerLogic(getIngredient)

def getIngredient(ingredientId: IngredientId):
ZIO[IIngredientRepo, IngredientError.NotFound, Ingredient] =
  ZIO.serviceWithZIO[IIngredientRepo](_.getById(ingredientId))
