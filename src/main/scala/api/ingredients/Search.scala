package api.ingredients

import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.*
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

case class IngredientSearchResult(
  id: IngredientId,
  name: String,
  available: Boolean
) extends Searchable

case class SearchResultsResp(
  results: Vector[IngredientSearchResult],
  found: Int
)

private type SearchEnv = IngredientsRepo & StorageIngredientsRepo

private val search: ZServerEndpoint[SearchEnv, Any] =
  endpoint
    .get
    .inSearchParams
    .in(query[StorageId]("storage-id"))
    .out(jsonBody[SearchResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchHandler)

// TODO this should be authenticated
private def searchHandler(
  si: SearchParams,
  storageId: StorageId,
): ZIO[SearchEnv, InternalServerError, SearchResultsResp] =
  for
    allIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_.getAll.mapError(_ => InternalServerError()))
    allIngredientsAvailability <- ZIO.foreach(allIngredients) {
      ingredient =>
        ZIO.serviceWithZIO[StorageIngredientsRepo](_.inStorage(storageId, ingredient.id))
          .map(inStorage => IngredientSearchResult(ingredient.id, ingredient.name, inStorage))
          .mapError(_ => InternalServerError())
    }
    res = Searchable.search(
      allIngredientsAvailability, si.query, si.size, si.offset, si.threshold
    )

  yield SearchResultsResp(res.slice(si.offset, si.offset + si.size), res.length)
