package api.ingredients

import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError, IngredientNotFound}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetEnv = IngredientsRepo

private val get: ZServerEndpoint[GetEnv, Any] =
  ingredientsEndpoint
  .get
  .in(path[IngredientId]("ingredientId"))
  .out(jsonBody[IngredientResp])
  .out(statusCode(StatusCode.Ok))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zServerLogic(getHandler)

private def getHandler(ingredientId: IngredientId):
  ZIO[GetEnv, InternalServerError | IngredientNotFound, IngredientResp] =
  {
    for
      mIngredient <- ZIO.serviceWithZIO[IngredientsRepo](_.getById(ingredientId))
      ingredient <- ZIO.fromOption(mIngredient)
        .orElseFail(IngredientNotFound(ingredientId.toString))
    yield IngredientResp.fromDb(ingredient)
  }.mapError {
    case e: IngredientNotFound => e
    case _ => InternalServerError()
  }

