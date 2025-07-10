package api.ingredients.personal

import sttp.tapir.ztapir.*

private val privateIngredientsEndpoint =
  endpoint
    .in("my" / "ingredients")
