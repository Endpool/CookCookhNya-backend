package api.recipes

import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.*
import db.repositories.{RecipesRepo, StorageIngredientsRepo}
import domain.InternalServerError
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

case class RecipeSearchResp(name: String, sourceLink: String) extends Searchable
case class SearchAllRecipesResp(
                                 results: Vector[RecipeSearchResp],
                                 found: Int
                               )

private type SearchAllEnv = RecipesRepo & StorageIngredientsRepo

private val searchAll: ZServerEndpoint[SearchAllEnv, Any] =
  recipesEndpoint
    .get
    .inSearchParams
    .out(jsonBody[SearchAllRecipesResp])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchAllRecipesHandler)

private def searchAllRecipesHandler(sp: SearchParams):
  ZIO[SearchAllEnv, InternalServerError, SearchAllRecipesResp] =
  for
    allDbRecipes <- ZIO.serviceWithZIO[RecipesRepo] (_.getAll.mapError(_ => InternalServerError()))
    allRecipes= allDbRecipes.map(dbRecipe => RecipeSearchResp(dbRecipe.name, dbRecipe.sourceLink))
    res = Searchable.search(allRecipes, sp.query, sp.size, sp.offset, sp.threshold)
  yield SearchAllRecipesResp(res.slice(sp.offset, sp.offset + sp.size), res.length)
