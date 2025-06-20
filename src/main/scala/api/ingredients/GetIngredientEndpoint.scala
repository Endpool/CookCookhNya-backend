package api.ingredients

import api.AppEnv
import db.repositories.IIngredientsRepo
import domain.{Ingredient, IngredientError, IngredientId}
import api.GeneralEndpointData.ingredientNotFoundVariant

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
  ZIO[IIngredientsRepo, IngredientError.NotFound, Ingredient] =
  ZIO.serviceWithZIO[IIngredientsRepo](_.getById(ingredientId))
