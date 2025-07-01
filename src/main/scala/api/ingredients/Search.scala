package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo, StorageMembersRepo}
import domain.{IngredientId, InternalServerError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import me.xdrop.fuzzywuzzy.FuzzySearch

private case class IngredientSearchResult(
                                         id: IngredientId,
                                         name: String,
                                         available: Boolean
                                         )

private case class SearchResults(
                                  results: Vector[IngredientSearchResult],
                                  found: Int
                                )

private val search: ZServerEndpoint[AppEnv, Any] =
  endpoint
    .get
    .in("ingredients-for-storage")
    .in(query[String]("query"))
    .in(query[StorageId]("storage"))
    .in(query[Int]("size").default(2))
    .in(query[Int]("offset").default(0))
    .out(jsonBody[SearchResults])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchHandler)

private def searchHandler(
                           query: String,
                           storageId: StorageId,
                           size: Int,
                           offset: Int
                         ):
  ZIO[IngredientsRepo & StorageIngredientsRepo & StorageMembersRepo, InternalServerError, SearchResults] =
  for
    allIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_.getAll.mapError(_ => InternalServerError()))
    allIngredientsAvailability <- ZIO.foreach(allIngredients) {
      ingredient =>
        ZIO.serviceWithZIO[StorageIngredientsRepo](_.inStorage(storageId, ingredient.id))
          .map(inStorage => IngredientSearchResult(ingredient.id, ingredient.name, inStorage))
          .mapError(_ => InternalServerError())
    }
    res = allIngredientsAvailability
      .map(i => (i, FuzzySearch.tokenSetPartialRatio(query, i.name)))
      .filter((_, ratio) => ratio >= 70) // 70 is the min ratio threshold
      .sortBy(
        (i, ratio) => (
          - ratio, // negate the ratio to make order descending
          (i.name.length - query.length).abs // secondary sorting is performed by length difference
        )
      )
      .map(_._1)

  yield SearchResults(res.slice(offset, offset + size), res.length)