package api.ingredients

case class SearchAllResultsResp(
  results: Vector[IngredientResp],
  found: Int
)