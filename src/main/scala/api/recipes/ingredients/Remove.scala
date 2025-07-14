package api.recipes.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  recipeNotFoundVariant,
}
import db.repositories.{IngredientsRepo, RecipeIngredientsRepo, RecipesRepo}
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound}

import sttp.model.StatusCode.NoContent
import sttp.tapir.ztapir.*
import zio.ZIO

private type RemoveEnv = RecipeIngredientsRepo & IngredientsRepo & RecipesRepo

private val remove: ZServerEndpoint[RemoveEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(NoContent))
    .errorOut(oneOf(
      recipeNotFoundVariant,
      CannotModifyPublishedRecipe.variant,
      serverErrorVariant,
    ))
    .zSecuredServerLogic(removeHandler)

private def removeHandler(recipeId : RecipeId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & RemoveEnv,
      InternalServerError | RecipeNotFound | CannotModifyPublishedRecipe,
      Unit] = for
  recipeIsVisible <- ZIO.serviceWithZIO[RecipesRepo](_
    .isVisible(recipeId)
    .orElseFail(InternalServerError())
  )
  _ <- ZIO.fail(RecipeNotFound(recipeId))
    .unless(recipeIsVisible)

  recipeIsPublic <- ZIO.serviceWithZIO[RecipesRepo](_
    .isPublic(recipeId)
    .orElseFail(InternalServerError())
  )
  _ <- ZIO.fail(CannotModifyPublishedRecipe(recipeId))
    .when(recipeIsPublic)

  _ <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
    .removeIngredient(recipeId, ingredientId)
    .orElseFail(InternalServerError())
  )
yield ()

