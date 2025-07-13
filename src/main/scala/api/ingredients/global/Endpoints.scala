package api.ingredients.global

import api.ingredients.ingredientsEndpoint
import api.TapirExtensions.superTag

import sttp.tapir.ztapir.*

val globalIngredientsEndpoint =
  ingredientsEndpoint
    .superTag("Public")
    .prependIn("public")

val globalEndpoints = List(
  get.widen,
  search.widen,
  // createGlobal.widen,
  // deleteGlobal.widen,
  // searchForStorage.widen,
  // searchForRecipe.widen,
)
