package api.ingredients.global

import sttp.tapir.ztapir.*

val globalIngredientsEndpoint =
  endpoint
    .in("ingredients")
