package api.ingredients

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.ingredients.public.publicEndpoints
import api.TapirExtensions.subTag

val ingredientsEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  ingredientsEndpoint()

def ingredientsEndpoint(path: String = "ingredients"): Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .subTag("Ingredients")
    .in(path)

val ingredientsEndpoints = List(
  create.widen,
  delete.widen,
  get.widen,
  search.widen,
  searchForRecipe.widen,
  searchForStorage.widen,
  requestPublication.widen
) ++ publicEndpoints.map(_.widen)
