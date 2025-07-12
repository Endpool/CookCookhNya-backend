package api.shoppinglist

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.serverErrorVariant
import domain.{IngredientId, InternalServerError}
import db.repositories.ShoppingListsRepo

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type DeleteIngredientsEnv = ShoppingListsRepo

private val deleteIngredients: ZServerEndpoint[DeleteIngredientsEnv, Any] = shoppingListEndpoint
  .delete
  .in(query[Vector[IngredientId]]("ingredient-id"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(deleteIngredientsHandler)

private def deleteIngredientsHandler(ingredients: Vector[IngredientId]):
  ZIO[AuthenticatedUser & DeleteIngredientsEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[ShoppingListsRepo](_
    .deleteIngredients(ingredients)
    .orElseFail(InternalServerError())
  )
