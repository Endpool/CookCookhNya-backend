package api.ingredients.global

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.*
import api.EndpointErrorVariants.serverErrorVariant
import db.QuillConfig.provideDS
import db.repositories.IngredientsQueries
import db.tables.DbStorageIngredient
import domain.{IngredientId, InternalServerError, StorageId}

import io.circe.generic.auto.*
import io.getquill
import io.getquill.{query => _, *}
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchForStorageEnv = DataSource

private val searchForStorage: ZServerEndpoint[SearchForStorageEnv, Any] =
  endpoint
  .get
  .in("ingredients-for-storage")
  .in(SearchParams.query)
  .in(PaginationParams.query)
  .in(query[StorageId]("storage-id"))
  .out(jsonBody[SearchResultsResp])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(searchForStorageHandler)

private def searchForStorageHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
  storageId: StorageId,
): ZIO[AuthenticatedUser & SearchForStorageEnv, InternalServerError, SearchResultsResp] =
  import db.QuillConfig.ctx.*
  for
    dataSource <- ZIO.service[DataSource]
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    allIngredientsAvailability <- run(
      IngredientsQueries.getAllVisibleQ(lift(userId))
        .leftJoin(getquill.query[DbStorageIngredient])
        .on((i, si) => i.id == si.ingredientId && si.storageId == lift(storageId))
        .map((i, si) => IngredientSearchResult(i.id, i.name, si.map(_.storageId).isDefined))
    ).provideDS(using dataSource)
      .map(Vector.from)
      .orElseFail(InternalServerError())
    res = Searchable.search(allIngredientsAvailability, searchParams)
  yield SearchResultsResp(res.paginate(paginationParams), res.length)
