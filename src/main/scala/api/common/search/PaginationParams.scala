package api.common.search

import sttp.tapir

case class PaginationParams(
  size: Int,
  offset: Int,
)

object PaginationParams:
  val query: tapir.EndpointInput[PaginationParams] =
    tapir.query[Int]("size").default(2).and(tapir.query[Int]("offset").default(0))
      .map
        (PaginationParams.apply.tupled)
        {case PaginationParams(size, offset) => (size, offset)}

