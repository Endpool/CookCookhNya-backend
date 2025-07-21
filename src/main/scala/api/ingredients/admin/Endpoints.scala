package api.ingredients.admin

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.ingredients.ingredientsEndpoint
import api.TapirExtensions.superTag

val adminIngredientsEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  ingredientsEndpoint
    .prependIn("admin")
    .superTag("Admin")

val adminIngredientsEndpoints = List(
  createPublic.widen,
)
