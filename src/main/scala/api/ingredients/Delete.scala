package api.ingredients

import api.AppEnv
import api.GeneralEndpointData.ingredientNotFoundVariant
import db.repositories.IIngredientsRepo
import domain.{IngredientError, IngredientId}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
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
  ZIO[IIngredientsRepo, IngredientError.NotFound, Unit] =
  ZIO.serviceWithZIO[IIngredientsRepo] {
    _.removeById(ingredientId)
  }
