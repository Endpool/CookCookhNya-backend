package api.ingredients

final case class SearchResp[A](
  results: Vector[A],
  found: Int
)

