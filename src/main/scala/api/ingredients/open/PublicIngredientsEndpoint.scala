package api.ingredients.open

import sttp.tapir.ztapir.*

val publicIngredientsEndpoint =
  endpoint
    .in("ingredients")