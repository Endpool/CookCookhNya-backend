package api.common.search

import me.xdrop.fuzzywuzzy.FuzzySearch

trait Searchable:
  val name: String

object Searchable:
  def search[A <: Searchable](
    searchables: Vector[A],
    searchParams: SearchParams
  ): Vector[A] =
    val SearchParams(query, threshold) = searchParams
    if query.isEmpty
      then searchables
      else searchables
        .map(i => (i, FuzzySearch.tokenSetPartialRatio(query, i.name)))
        .filter((_, ratio) => ratio >= threshold)
        .sortBy(
          (i, ratio) => (
            -ratio, // negate the ratio to make order descending
            (i.name.length - query.length).abs // secondary sort by length difference
          )
        )
        .map(_._1)
