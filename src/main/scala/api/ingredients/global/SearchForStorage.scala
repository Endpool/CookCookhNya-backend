package api.ingredients.global

import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.*
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class IngredientSearchResult(
  id: IngredientId,
  name: String,
  available: Boolean
) extends Searchable

case class SearchResultsResp(
  results: Vector[IngredientSearchResult],
  found: Int
)

private type SearchForStorageEnv = IngredientsRepo & StorageIngredientsRepo

private val searchForStorage: ZServerEndpoint[SearchForStorageEnv, Any] =
  endpoint
  .get
  .in("ingredients-for-storage")
  .in(SearchParams.query)
  .in(PaginationParams.query)
  .in(query[StorageId]("storage-id"))
  .out(jsonBody[SearchResultsResp])
  .errorOut(oneOf(serverErrorVariant))
  .zServerLogic(searchForStorageHandler)

private def searchForStorageHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
  storageId: StorageId,
): ZIO[SearchForStorageEnv, InternalServerError, SearchResultsResp] =
  for
    allIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_
      .getAllGlobal
      .orElseFail(InternalServerError())
    )
    allIngredientsAvailability <- ZIO.foreach(allIngredients) { ingredient =>
      ZIO.serviceWithZIO[StorageIngredientsRepo](_
        .inStorage(storageId, ingredient.id)
        .map(inStorage => IngredientSearchResult(ingredient.id, ingredient.name, inStorage))
        .orElseFail(InternalServerError())
      )
    }
    res = Searchable.search(allIngredientsAvailability, searchParams)
  yield SearchResultsResp(res.paginate(paginationParams), res.length)
