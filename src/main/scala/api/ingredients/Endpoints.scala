package api.ingredients

import api.ingredients.global.globalEndpoints
import api.ingredients.personal.personalEndpoints

val ingredientsEndpoints = globalEndpoints ++ personalEndpoints
