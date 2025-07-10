package api.recipes

import sttp.tapir.ztapir.*

val recipesEndpoint =
  endpoint
    .tag("Recipes")
    .in("recipes")

val recipeEndpoints = List(
  create.widen,
  getSuggested.widen,
  get.widen,
)
