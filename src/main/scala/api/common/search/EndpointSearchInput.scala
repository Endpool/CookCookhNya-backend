package api.common.search

import sttp.tapir.{Endpoint, query}

extension [ERROR, OUTPUT, R](endpoint: Endpoint[Unit, Unit, ERROR, OUTPUT, R])
  def searchInput: Endpoint[Unit, (String, Int, Int, Int), ERROR, OUTPUT, R] =
    endpoint
      .in(query[String]("query"))
      .in(query[Int]("size").default(2))
      .in(query[Int]("offset").default(0))
      .in(query[Int]("threshold").default(50))
