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
import domain.RecipeNotFound
import db.repositories.RecipesRepo
import api.EndpointErrorVariants.recipeNotFoundVariant

final case class CannotModifyPublishedRecipe(
  recipeId: RecipeId,
  messae: String = "Cannot modify published recipe",
)

private type AddEnv = RecipeIngredientsRepo & IngredientsRepo & RecipesRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(
      ingredientNotFoundVariant,
      recipeNotFoundVariant,
      serverErrorVariant,
    ))
    .zSecuredServerLogic(addHandler)

private def addHandler(recipeId : RecipeId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError | IngredientNotFound | RecipeNotFound,
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

  _ <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_
    .addIngredient(recipeId, ingredientId)
    .orElseFail(InternalServerError())
  )
yield ()

