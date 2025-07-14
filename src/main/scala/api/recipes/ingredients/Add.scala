package api.recipes.ingredients

import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import db.repositories.RecipeIngredientsRepo
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type AddEnv = RecipeIngredientsRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(
      serverErrorVariant,
      ingredientNotFoundVariant,
      storageNotFoundVariant,
    ))
    .zSecuredServerLogic(addHandler)

private def addHandler(recipeId : RecipeId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError,
      Unit] =
  ZIO.serviceWithZIO[RecipeIngredientsRepo](_
    .addIngredient(recipeId, ingredientId)
    .orElseFail(InternalServerError())
  )

