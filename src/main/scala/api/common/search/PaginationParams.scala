package api.common.search

import scala.collection.immutable.IndexedSeqOps
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

extension[A, CC[_], C](seq: IndexedSeqOps[A, CC, C])
  def paginate(paginationParams: PaginationParams): C =
    seq.slice(paginationParams.offset, paginationParams.offset + paginationParams.size)

