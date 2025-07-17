package api.recipes

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.recipes.ingredients.recipesIngredientsEndpoints

val recipesEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  recipesEndpoint()

def recipesEndpoint(path: String = "recipes"): Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .tag("Recipes")
    .in(path)

val recipeEndpoints = List(
  create.widen,
  getSuggested.widen,
  get.widen,
  searchAll.widen,
  delete.widen,
  requestPublication.widen,
) ++ recipesIngredientsEndpoints.map(_.widen)
