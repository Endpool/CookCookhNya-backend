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
    .inSearchParams
    .out(jsonBody[SearchAllResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchAllHandler)

private def searchAllHandler(sp: SearchParams):
  ZIO[SearchAllEnv, InternalServerError, SearchAllResultsResp] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo] (_.getAll.mapError(_ => InternalServerError()))
    allIngredients = allDbIngredients.map(dbIngredient => IngredientResp(dbIngredient.id, dbIngredient.name))
    res = Searchable.search(allIngredients, sp.query, sp.size, sp.offset, sp.threshold)
  yield SearchAllResultsResp(res.slice(sp.offset, sp.offset + sp.size), res.length)

