package api.ingredients.global

import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteEnv = IngredientsRepo

private val deleteGlobal: ZServerEndpoint[DeleteEnv, Any] =
  globalIngredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zServerLogic(deleteGlobalHandler)

private def deleteGlobalHandler(ingredientId: IngredientId):
  ZIO[DeleteEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .removeGlobal(ingredientId)
    .orElseFail(InternalServerError())
  )
