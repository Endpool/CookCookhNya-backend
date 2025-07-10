package api.ingredients.global

import api.common.search.*
import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.{IngredientResp, SearchAllResultsResp}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchAllEnv = IngredientsRepo

private val searchGlobal: ZServerEndpoint[SearchAllEnv, Any] =
  globalIngredientsEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchAllResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchGlobalHandler)

private def searchGlobalHandler(searchParams: SearchParams, paginationParams: PaginationParams):
  ZIO[SearchAllEnv, InternalServerError, SearchAllResultsResp] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_
      .getAllGlobal
      .orElseFail(InternalServerError())
    )
    allIngredients = allDbIngredients.map(IngredientResp.fromDb)
    res = Searchable.search(allIngredients, searchParams)
  yield SearchAllResultsResp(res.paginate(paginationParams), res.length)
