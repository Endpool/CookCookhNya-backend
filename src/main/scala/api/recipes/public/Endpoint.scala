package api.recipes.public

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.recipes.recipesEndpoint
import api.TapirExtensions.superTag

val publicRecipesEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  recipesEndpoint
    .prependIn("public")
    .superTag("Public")

val publicRecipesEndpoints = List(
  getPublic.widen,
  searchPublic.widen,
)
