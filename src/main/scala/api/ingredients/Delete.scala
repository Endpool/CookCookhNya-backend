package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant
}
import db.repositories.IngredientsRepo
import domain.{InternalServerError, IngredientId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val delete: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zServerLogic(deleteHandler)

private def deleteHandler(ingredientId: IngredientId):
  ZIO[IngredientsRepo, InternalServerError, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo] {
    _.removeById(ingredientId).mapError {
      _ => InternalServerError()
    }
  }
