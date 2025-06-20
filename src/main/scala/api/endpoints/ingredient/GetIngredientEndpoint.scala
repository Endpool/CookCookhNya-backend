package api.endpoints.ingredient

import api.db.repositories.IngredientRepoInterface
import api.domain.{Ingredient, IngredientError, IngredientId}
import api.endpoints.GeneralEndpointData.ingredientNotFoundVariant

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getIngredientEndpoint = endpoint
  .get
  .in("ingredients" / path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Ingredient])
  .errorOut(oneOf(ingredientNotFoundVariant))
  .zServerLogic(getIngredient)

def getIngredient(ingredientId: IngredientId):
ZIO[IngredientRepoInterface, IngredientError.NotFound, Ingredient] =
  ZIO.serviceWithZIO[IngredientRepoInterface](_.getById(ingredientId))
