package api.ingredients

import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.*
import api.ingredients.{IngredientResp, ingredientsEndpoint}
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.{query, *}
import zio.ZIO

case class SearchAllResultsResp(
  results: Vector[IngredientResp],
  found: Int
)

private type SearchAllEnv = IngredientsRepo & StorageIngredientsRepo

private val searchAll: ZServerEndpoint[SearchAllEnv, Any] =
  ingredientsEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchAllResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchAllHandler)

private def searchAllHandler(searchParams: SearchParams, pagination: PaginationParams):
  ZIO[SearchAllEnv, InternalServerError, SearchAllResultsResp] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_
      .getAll
      .orElseFail(InternalServerError())
    )
    allIngredients = allDbIngredients.map(IngredientResp.fromDb)
    res = Searchable.search(allIngredients, searchParams)
  yield SearchAllResultsResp(res.slice(pagination.offset, pagination.offset + pagination.size), res.length)

