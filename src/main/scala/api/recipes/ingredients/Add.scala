package api.recipes.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  recipeNotFoundVariant,
}
import db.repositories.{IngredientsRepo, RecipeIngredientsRepo, RecipesRepo}
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId, RecipeNotFound}

import sttp.model.StatusCode.NoContent
import sttp.tapir.ztapir.*
import zio.ZIO

private type AddEnv = RecipeIngredientsRepo & IngredientsRepo & RecipesRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(NoContent))
    .errorOut(oneOf(
      ingredientNotFoundVariant,
      recipeNotFoundVariant,
      CannotModifyPublishedRecipe.variant,
      serverErrorVariant,
    ))
    .zSecuredServerLogic(addHandler)

private def addHandler(recipeId : RecipeId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError | IngredientNotFound | RecipeNotFound | CannotModifyPublishedRecipe,
      Unit] = for
  ingredientIsVisible <- ZIO.serviceWithZIO[IngredientsRepo](_
    .isVisible(ingredientId)
    .orElseFail(InternalServerError())
  )
  _ <- ZIO.fail(IngredientNotFound(ingredientId.toString()))
    .unless(ingredientIsVisible)

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
    .addIngredient(recipeId, ingredientId)
    .orElseFail(InternalServerError())
  )
yield ()

