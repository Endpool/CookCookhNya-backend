package api.ingredients.personal

import api.ingredients.IngredientResp
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, IngredientNotFound, InternalServerError}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetEnv = IngredientsRepo

private[ingredients] val getPrivate: ZServerEndpoint[GetEnv, Any] =
  privateIngredientsEndpoint
    .get
    .in(path[IngredientId]("ingredientId"))
    .out(jsonBody[IngredientResp])
    .out(statusCode(StatusCode.Ok))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(getPrivateHandler)

private def getPrivateHandler(ingredientId: IngredientId):
ZIO[AuthenticatedUser & GetEnv, InternalServerError | IngredientNotFound, IngredientResp] =
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
