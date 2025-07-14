package api.recipes.ingredients

import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import db.repositories.RecipeIngredientsRepo
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO
import db.repositories.IngredientsRepo

private type AddEnv = RecipeIngredientsRepo & IngredientsRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(
      ingredientNotFoundVariant,
      serverErrorVariant,
    ))
    .zSecuredServerLogic(addHandler)

private def addHandler(recipeId : RecipeId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError | IngredientNotFound,
      Unit] = for
  ingredientIsVisible <- ZIO.serviceWithZIO[IngredientsRepo](_
    .isVisible(ingredientId)
    .orElseFail(InternalServerError())
  )
  _ <- ZIO.fail(IngredientNotFound(ingredientId.toString()))
    .unless(ingredientIsVisible)
  _ <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
    .addIngredient(recipeId, ingredientId)
    .orElseFail(InternalServerError())
  )
yield ()

