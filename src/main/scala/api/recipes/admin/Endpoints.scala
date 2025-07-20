package api.recipes.admin

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.recipes.recipesEndpoint
import api.TapirExtensions.superTag

val adminRecipesEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  recipesEndpoint
    .prependIn("admin")
    .superTag("Admin")

val adminRecipesEndpoints = List(
  createPublic.widen,
)
