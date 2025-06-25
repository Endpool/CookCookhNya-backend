package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.ingredientNotFoundVariant
import db.repositories.IngredientsRepo
import domain.{IngredientError, IngredientId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val delete: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .delete
  .in(path[IngredientId]("ingredientId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(ingredientNotFoundVariant))
  .zServerLogic(deleteHandler)

private def deleteHandler(ingredientId: IngredientId):
  ZIO[IngredientsRepo, IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IngredientsRepo] {
    _.removeById(ingredientId).catchAll(???)
  }
