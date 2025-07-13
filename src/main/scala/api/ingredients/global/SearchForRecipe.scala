package api.ingredients.global

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.*
import api.EndpointErrorVariants.serverErrorVariant
import db.QuillConfig.provideDS
import db.repositories.IngredientsQueries
import db.tables.DbRecipeIngredient
import domain.{IngredientId, InternalServerError, RecipeId}

import io.circe.generic.auto.*
import io.getquill
import io.getquill.{query => _, *}
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchForRecipeEnv = DataSource

private val searchForRecipe: ZServerEndpoint[SearchForRecipeEnv, Any] =
  endpoint
    .get
    .in("ingredients-for-recipe")
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .in(query[RecipeId]("recipe-id"))
    .out(jsonBody[SearchResultsResp])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchForRecipeHandler)

private def searchForRecipeHandler(
                                     searchParams: SearchParams,
                                     paginationParams: PaginationParams,
                                     recipeId: RecipeId
                                   ): ZIO[AuthenticatedUser & SearchForRecipeEnv, InternalServerError, SearchResultsResp] =
  import db.QuillConfig.ctx.*
  for
    dataSource <- ZIO.service[DataSource]
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    allIngredientsAvailability <- run(
      IngredientsQueries.getAllVisibleQ(lift(userId))
        .leftJoin(getquill.query[DbRecipeIngredient])
        .on((i, ri) => i.id == ri.ingredientId && ri.recipeId == lift(recipeId))
        .map((i, ri) => IngredientSearchResult(i.id, i.name, ri.map(_.recipeId).isDefined))
    ).provideDS(using dataSource)
      .map(Vector.from)
      .orElseFail(InternalServerError())
    res = Searchable.search(allIngredientsAvailability, searchParams)
  yield SearchResultsResp(res.paginate(paginationParams), res.length)
