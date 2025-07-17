package api.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.common.search.{PaginationParams, paginate, SearchParams, Searchable}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.{Codec, Schema, Validator, EndpointInput}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

enum SearchIngredientsFilter:
  case Custom, Public, All

object SearchIngredientsFilter:
  val query: EndpointInput.Query[SearchIngredientsFilter] = query()
  def query(default: SearchIngredientsFilter = SearchIngredientsFilter.All):
    EndpointInput.Query[SearchIngredientsFilter] =
    sttp.tapir.query[SearchIngredientsFilter]("filter").default(default)

  given Codec.PlainCodec[SearchIngredientsFilter] =
    Codec.derivedEnumeration.defaultStringBased

private type SearchEnv = IngredientsRepo & StorageIngredientsRepo

private val search: ZServerEndpoint[SearchEnv, Any] =
  ingredientsEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .in(SearchIngredientsFilter.query)
    .out(jsonBody[SearchResp[IngredientResp]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchHandler)

private def searchHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
  filter: SearchIngredientsFilter,
): ZIO[AuthenticatedUser & SearchEnv, InternalServerError, SearchResp[IngredientResp]] =
  for
    getIngredients <- ZIO.serviceWith[IngredientsRepo](filter match
      case SearchIngredientsFilter.Custom => _.getAllCustom
      case SearchIngredientsFilter.Public => _.getAllPublic
      case SearchIngredientsFilter.All    => _.getAll
    )
    allIngredients <- getIngredients
      .map(_.map(IngredientResp.fromDb))
      .orElseFail(InternalServerError())
    res = Searchable.search(Vector.from(allIngredients), searchParams)
  yield SearchResp(res.paginate(paginationParams), res.length)