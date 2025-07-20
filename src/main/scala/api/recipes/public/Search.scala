package api.recipes.public

import api.common.search.*
import api.recipes.{SearchAllRecipesResp, RecipeSearchResp}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{RecipesRepo, StorageIngredientsRepo}
import db.tables.DbRecipe
import domain.{RecipeId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type SearchPublicEnv = RecipesRepo & StorageIngredientsRepo

private val searchPublic: ZServerEndpoint[SearchPublicEnv, Any] =
  publicRecipesEndpoint
    .get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchAllRecipesResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchPublicHandler)

private def searchPublicHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
): ZIO[SearchPublicEnv, InternalServerError, SearchAllRecipesResp] = for
  allDbRecipes <- ZIO.serviceWithZIO[RecipesRepo](_
    .getAllPublic
    .map(Vector.from)
    .orElseFail(InternalServerError())
  )
  allRecipes = allDbRecipes.map(RecipeSearchResp.fromDb)
  res = Searchable.search(allRecipes, searchParams)
yield SearchAllRecipesResp(res.paginate(paginationParams), res.length)
