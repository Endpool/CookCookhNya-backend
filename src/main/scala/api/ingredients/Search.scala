package api.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.common.search.{PaginationParams, paginate, SearchParams, Searchable}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchEnv = IngredientsRepo & StorageIngredientsRepo

private val search: ZServerEndpoint[SearchEnv, Any] =
  ingredientsEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchResp[IngredientResp]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchHandler)

private def searchHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
): ZIO[AuthenticatedUser & SearchEnv, InternalServerError, SearchResp[IngredientResp]] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_
      .getAllVisible
      .orElseFail(InternalServerError())
    )
    allIngredients = allDbIngredients.map(IngredientResp.fromDb)
    res = Searchable.search(Vector.from(allIngredients), searchParams)
  yield SearchResp(res.paginate(paginationParams), res.length)
