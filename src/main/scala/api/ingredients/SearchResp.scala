package api.ingredients

import api.common.search.Searchable
import domain.IngredientId

final case class IngredientSearchResult(
  id: IngredientId,
  name: String,
  available: Boolean
) extends Searchable

case class SearchResp[A](
  results: Vector[A],
  found: Int
)

