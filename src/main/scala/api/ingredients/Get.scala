package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}
import domain.IngredientError.NotFound
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
  .out(jsonBody[IngredientResp])
  .out(statusCode(StatusCode.Ok))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zServerLogic(getHandler)

private def getHandler(ingredientId: IngredientId):
  ZIO[IngredientsRepo, InternalServerError | NotFound, IngredientResp] = 
  {
    for
      mIngredient <- ZIO.serviceWithZIO[IngredientsRepo](_.getById(ingredientId))
      ingredient <- ZIO.fromOption(mIngredient)
        .orElseFail(NotFound(ingredientId))
    yield IngredientResp.fromDb(ingredient)
  }.catchAll {
    case e: NotFound => ZIO.fail(e)
    case _ => ZIO.fail(InternalServerError())
  }

