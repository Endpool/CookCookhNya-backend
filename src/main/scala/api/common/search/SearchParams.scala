package api.common.search

import sttp.tapir.{Codec, Schema}

case class SearchParams(
                        query: String,
                        size: Int,
                        offset: Int,
                        threshold: Int
                      )

extension [A](handler: SearchParams => A)
  def inputToSearchParams: ((String, Int, Int, Int)) => A = handler.compose(SearchParams.apply.tupled)