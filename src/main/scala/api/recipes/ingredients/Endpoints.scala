package api.recipes.ingredients

import api.recipes.recipesEndpoint
import api.TapirExtensions.subTag
import domain.RecipeId

import sttp.tapir.ztapir.*

val recipesIngredientsEndpoint =
  recipesEndpoint
    .subTag("Ingredients")
    .in(path[RecipeId]("recipeId") / "ingredients")

val recipesIngredientsEndpoints = List(
  add.widen,
)
