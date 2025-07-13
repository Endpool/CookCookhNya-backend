package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.*
import db.repositories.{RecipesRepo, StorageIngredientsRepo}
import domain.InternalServerError

import io.circe.generic.auto.*
import sttp.tapir.{Codec, Schema, Validator, EndpointInput}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

case class RecipeSearchResp(
  name: String,
  sourceLink: Option[String],
) extends Searchable

case class SearchAllRecipesResp(
  results: Vector[RecipeSearchResp],
  found: Int,
)

enum SearchRecipesFilter:
  case Custom, Public, All

object SearchRecipesFilter:
  val query: EndpointInput.Query[SearchRecipesFilter] = query()
  def query(default: SearchRecipesFilter = SearchRecipesFilter.All):
    EndpointInput.Query[SearchRecipesFilter] =
    sttp.tapir.query[SearchRecipesFilter]("filter").default(default)

  given Codec.PlainCodec[SearchRecipesFilter] =
    Codec.derivedEnumeration.defaultStringBased

private type SearchEnv = RecipesRepo & StorageIngredientsRepo

private val searchAll: ZServerEndpoint[SearchEnv, Any] =
  recipesEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .in(SearchRecipesFilter.query)
    .out(jsonBody[SearchAllRecipesResp])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchAllRecipesHandler)

private def searchAllRecipesHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
  filter: SearchRecipesFilter,
): ZIO[AuthenticatedUser & SearchEnv, InternalServerError, SearchAllRecipesResp] =
  for
    getRecipes <- ZIO.serviceWith[RecipesRepo](filter match
      case SearchRecipesFilter.Custom => _.getAllCustom
      case SearchRecipesFilter.Public => _.getAllPublic
      case SearchRecipesFilter.All    => _.getAll
    )
    allDbRecipes <- getRecipes
      .map(Vector.from)
      .orElseFail(InternalServerError())
    allRecipes = allDbRecipes.map(dbRecipe => RecipeSearchResp(dbRecipe.name, dbRecipe.sourceLink))
    res = Searchable.search(allRecipes, searchParams)
  yield SearchAllRecipesResp(res.paginate(paginationParams), res.length)
