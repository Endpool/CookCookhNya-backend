package api.ingredients.global

import api.common.search.Searchable
import domain.IngredientId

final case class IngredientSearchResult(
                                         id: IngredientId,
                                         name: String,
                                         available: Boolean
                                       ) extends Searchable

case class SearchResultsResp(
                              results: Vector[IngredientSearchResult],
                              found: Int
                            )

