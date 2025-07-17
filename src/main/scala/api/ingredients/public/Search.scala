package api.ingredients.public

import api.common.search.*
import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.{SearchResp, IngredientResp}
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchAllEnv = IngredientsRepo

private val search: ZServerEndpoint[SearchAllEnv, Any] =
  publicIngredientsEndpint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchResp[IngredientResp]])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchHandler)

private def searchHandler(searchParams: SearchParams, paginationParams: PaginationParams):
  ZIO[SearchAllEnv, InternalServerError, SearchResp[IngredientResp]] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_
      .getAllPublic
      .orElseFail(InternalServerError())
    )
    allIngredients = Vector.from(allDbIngredients).map(IngredientResp.fromDb)
    res = Searchable.search(allIngredients, searchParams)
  yield SearchResp(res.paginate(paginationParams), res.length)
