package api.shoppinglist

import api.{handleFailedSqlQuery, toIngredientNotFound}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, ingredientNotFoundVariant}
import domain.{InternalServerError, IngredientId, UserId}
import domain.IngredientError.NotFound
import db.repositories.ShoppingListsRepo
import db.DbError.*

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type AddIngredientsEnv = ShoppingListsRepo

private val addIngredients: ZServerEndpoint[AddIngredientsEnv, Any] =
  shoppingListEndpoint
    .put
    .in(query[Vector[IngredientId]]("ingredient-id"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(addIngredientsHandler)

private def addIngredientsHandler(ingredients: Vector[IngredientId]):
  ZIO[AddIngredientsEnv, InternalServerError | NotFound, Unit] =
  val userId = ??? // TODO make ShoppingListsRepo require AuthenticatedUser from ZIO environment
  ZIO.serviceWithZIO[ShoppingListsRepo] {
    _.addIngredients(userId, ingredients)
  }.mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toIngredientNotFound)
      .getOrElse(InternalServerError())
  }
