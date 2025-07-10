package api.ingredients

import sttp.tapir.ztapir.*

import api.ingredients.global.globalEndpoints
import api.ingredients.personal.personalEndpoints

val ingredientsEndpoint =
  endpoint
    .tag("Ingredients")
    .in("ingredients")

val ingredientsEndpoints = globalEndpoints.map(_.widen) ++ personalEndpoints.map(_.widen)
