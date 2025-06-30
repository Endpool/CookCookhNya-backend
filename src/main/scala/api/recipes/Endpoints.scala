package api.recipes

import sttp.tapir.ztapir.*

val recipesEndpoint =
  endpoint
    .in("recipes")

val recipeEndpoints = List(
  create,
  getSuggested,
  get
)
