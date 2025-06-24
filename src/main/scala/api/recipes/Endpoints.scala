package api.recipes

import domain.UserId

import sttp.tapir.ztapir.*

val recipesEndpoint =
  endpoint
    .in("my" / "recipes")
    .securityIn(auth.bearer[UserId]())

val recipeEndpoints = List(
  create,
  getSuggested
)