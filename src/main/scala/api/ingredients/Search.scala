package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.{PaginationParams, SearchParams, Searchable, paginate}
import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.SearchIngredientsFilter.Custom
import api.PublicationRequestStatusResp
import db.repositories.{IngredientPublicationRequestsRepo, IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}
import io.circe.generic.auto.*
import sttp.tapir.{Codec, EndpointInput, Schema, Validator}
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

private type SearchEnv = IngredientsRepo & StorageIngredientsRepo & IngredientPublicationRequestsRepo

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
): ZIO[AuthenticatedUser & SearchEnv, InternalServerError, SearchResp[IngredientResp]] = {

  def getLastPublicationRequestStatus(ingredientId: IngredientId) =
    ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](
      _.getAllRequestsForIngredient(ingredientId).map(
        _.sortBy(_.updatedAt).lastOption.map(
          req => PublicationRequestStatusResp.fromDomain(req.status.toDomain(req.reason))
        )
      )
    )

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
    res <- ZIO.foreach(res) { resp =>
      getLastPublicationRequestStatus(resp.id)
        .map(status => resp.copy(moderationStatus = status))
        .orElseFail(InternalServerError())
    }.when(filter == Custom).someOrElse(res)
  yield SearchResp(res.paginate(paginationParams), res.length)
}
