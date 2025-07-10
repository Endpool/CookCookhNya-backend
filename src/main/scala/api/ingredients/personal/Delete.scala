package api.ingredients.personal

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteEnv = IngredientsRepo

private[ingredients] val deletePrivate: ZServerEndpoint[DeleteEnv, Any] =
  privateIngredientsEndpoint
    .delete
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(deletePrivateHandler)

private def deletePrivateHandler(ingredientId: IngredientId):
ZIO[AuthenticatedUser & DeleteEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo] {_
    .removeById(ingredientId)
    .orElseFail(InternalServerError())
  }
