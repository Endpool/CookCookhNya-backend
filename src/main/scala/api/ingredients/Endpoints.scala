package api.ingredients

import sttp.tapir.ztapir.*

import api.ingredients.global.globalEndpoints
import api.ingredients.personal.personalEndpoints

val ingredientsEndpoints = globalEndpoints.map(_.widen) ++ personalEndpoints.map(_.widen)
