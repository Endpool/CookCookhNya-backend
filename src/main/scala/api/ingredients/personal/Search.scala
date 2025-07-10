package api.ingredients.personal

import api.ingredients.IngredientSearchResult
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError, StorageId, UserId}
import io.circe.generic.auto.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

case class SearchResultsResp(
  results: Vector[IngredientSearchResult],
  found: Int
)

private type SearchEnv = IngredientsRepo & StorageIngredientsRepo

private[ingredients] val searchPrivate: ZServerEndpoint[SearchEnv, Any] =
  privateIngredientsEndpoint
    .in("for-storage")
    .get
    .in(query[String]("query"))
    .in(query[StorageId]("storage-id"))
    .in(query[Int]("size").default(2))
    .in(query[Int]("offset").default(0))
    .in(query[Int]("threshold").default(50))
    .out(jsonBody[SearchResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchPrivateHandler)

private def searchPrivateHandler(
  query: String,
  storageId: StorageId,
  size: Int,
  offset: Int,
  threshold: Int
): ZIO[AuthenticatedUser & SearchEnv, InternalServerError, SearchResultsResp] =
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
      .filter((_, ratio) => ratio >= threshold)
      .sortBy(
        (i, ratio) => (
          -ratio, // negate the ratio to make order descending
          (i.name.length - query.length).abs // secondary sort by length difference
        )
      )
      .map(_._1)

  yield SearchResultsResp(res.slice(offset, offset + size), res.length)
