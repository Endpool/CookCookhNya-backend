package api.shoppinglist

import api.AppEnv
import api.zSecuredServerLogic
import api.EndpointErrorVariants.serverErrorVariant
import domain.{IngredientId, InternalServerError, UserId}
import db.repositories.ShoppingListsRepo

import sttp.model.StatusCode
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{Exit, ZIO}

val delete: ZServerEndpoint[AppEnv, Any] = shoppingListEndpoint
  .delete
  .in(query[Vector[IngredientId]]("ingredient-id"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(deleteIngredients)

def deleteIngredients(userId: UserId)(ingredients: Vector[IngredientId]):
ZIO[ShoppingListsRepo, InternalServerError, Unit] =
  ZIO.serviceWithZIO[ShoppingListsRepo] {
    _.deleteIngredients(userId, ingredients)
  }.mapError(_ => InternalServerError())
