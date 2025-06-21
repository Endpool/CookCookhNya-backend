package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.ingredientNotFoundVariant
import db.repositories.IngredientsRepo
import domain.{Ingredient, IngredientError, IngredientId}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val get: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .get
  .in(path[IngredientId]("ingredientId"))
  .out(jsonBody[Ingredient])
  .out(statusCode(StatusCode.Ok))
  .errorOut(oneOf(ingredientNotFoundVariant))
  .zServerLogic(getHandler)

private def getHandler(ingredientId: IngredientId):
  ZIO[IngredientsRepo, IngredientError.NotFound, Ingredient] =
  ZIO.serviceWithZIO[IngredientsRepo](_.getById(ingredientId))
