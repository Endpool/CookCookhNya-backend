package api.ingredients.personal

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteEnv = IngredientsRepo

private val deletePersonal: ZServerEndpoint[DeleteEnv, Any] =
  personalIngredientsEndpoint
    .delete
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(deletePersonalHandler)

private def deletePersonalHandler(ingredientId: IngredientId):
ZIO[AuthenticatedUser & DeleteEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .removePersonal(ingredientId)
    .orElseFail(InternalServerError())
  )
