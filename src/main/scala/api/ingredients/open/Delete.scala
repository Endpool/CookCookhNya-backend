package api.ingredients.open

import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteEnv = IngredientsRepo

private[ingredients] val deletePublic: ZServerEndpoint[DeleteEnv, Any] =
  publicIngredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zServerLogic(deleteHandler)

private def deleteHandler(ingredientId: IngredientId):
  ZIO[DeleteEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo] {
    _.removePublicById(ingredientId).mapError {
      _ => InternalServerError()
    }
  }
