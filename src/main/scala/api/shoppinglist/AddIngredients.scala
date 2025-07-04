package api.shoppinglist

import api.{AppEnv, handleFailedSqlQuery, failIfIngredientNotFound}
import api.zSecuredServerLogic
import api.EndpointErrorVariants.{serverErrorVariant, ingredientNotFoundVariant}
import domain.{InternalServerError, IngredientId, UserId}
import domain.IngredientError.NotFound
import db.repositories.ShoppingListsRepo
import db.DbError.*

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private val addIngredients: ZServerEndpoint[AppEnv, Any] = shoppingListEndpoint
  .put
  .in(query[Vector[IngredientId]]("ingredient-id"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zSecuredServerLogic(addIngredientsHandler)

private def addIngredientsHandler(userId: UserId)(ingredients: Vector[IngredientId]):
ZIO[ShoppingListsRepo, InternalServerError | NotFound, Unit] =
  ZIO.serviceWithZIO[ShoppingListsRepo] {
    _.addIngredients(userId, ingredients)
  }.catchAll {
    case DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: FailedDbQuery => 
      for
        missingEntry <- handleFailedSqlQuery(e)
        (keyName, keyValue, _) = missingEntry
        _ <- failIfIngredientNotFound(keyName, keyValue)
        nothing <- ZIO.fail(InternalServerError())
      yield nothing
  }
