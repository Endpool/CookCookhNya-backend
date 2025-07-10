package api.common.search

import sttp.tapir.{Codec, Schema}

final case class SearchParams(
  query: String,
  size: Int,
  offset: Int,
  threshold: Int
)
