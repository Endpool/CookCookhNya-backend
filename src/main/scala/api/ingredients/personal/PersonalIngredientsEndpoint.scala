package api.ingredients.personal

import sttp.tapir.ztapir.*

private val personalIngredientsEndpoint =
  endpoint
    .in("my" / "ingredients")
