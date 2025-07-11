package api.common.search

import sttp.tapir

final case class SearchParams(
  query: String,
  threshold: Int
)

object SearchParams:
  val query: tapir.EndpointInput[SearchParams] =
    tapir.query[String]("query").and(tapir.query[Int]("threshold").default(50)).map
      (SearchParams.apply.tupled)
      {case SearchParams(query, threshold) => (query, threshold)}
