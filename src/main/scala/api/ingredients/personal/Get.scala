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

private val getPersonal: ZServerEndpoint[GetEnv, Any] =
  personalIngredientsEndpoint
    .get
    .in(path[IngredientId]("ingredientId"))
    .out(jsonBody[IngredientResp])
    .out(statusCode(StatusCode.Ok))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(getPersonalHandler)

private def getPersonalHandler(ingredientId: IngredientId):
ZIO[AuthenticatedUser & GetEnv, InternalServerError | IngredientNotFound, IngredientResp] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .getPersonal(ingredientId)
    .someOrFail(IngredientNotFound(ingredientId.toString))
    .map(IngredientResp.fromDb)
    .mapError {
      case e: IngredientNotFound => e
      case _ => InternalServerError()
    }
  )
