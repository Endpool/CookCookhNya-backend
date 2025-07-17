package api.ingredients.public

import api.ingredients.ingredientsEndpoint
import api.TapirExtensions.superTag

import sttp.tapir.ztapir.*

val publicIngredientsEndpint =
  ingredientsEndpoint
    .superTag("Public")
    .prependIn("public")

val publicEndpoints = List(
  get.widen,
  search.widen,
  // create.widen,
  // delete.widen,
)
