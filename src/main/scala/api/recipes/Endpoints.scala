package api.recipes

import domain.UserId

import sttp.tapir.ztapir.*

val recipesEndpoint =
  endpoint
    .in("recipes")

val recipeEndpoints = List(
  create,
  getSuggested
)
