package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.PaginationParams
import api.moderation.pubrequests.PublicationRequestType.*
import db.repositories.{
  IngredientPublicationRequestsRepo,
  IngredientsRepo,
  RecipePublicationRequestsRepo,
  RecipesRepo
}
import domain.{IngredientPublicationRequest, InternalServerError, RecipePublicationRequest}
import io.circe.generic.auto.*
import sttp.model.StatusCode.NoContent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetSomePendingEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo
  & RecipesRepo
  & IngredientsRepo

private type PublicationRequest = RecipePublicationRequest | IngredientPublicationRequest

private val getSomePending: ZServerEndpoint[GetSomePendingEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(PaginationParams.query)
    .out(statusCode(NoContent))
    .out(jsonBody[Seq[PublicationRequestSummary]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(getSomePendingHandler)

private def getSomePendingHandler(paginationParams: PaginationParams):
  ZIO[AuthenticatedUser & GetSomePendingEnv & RecipesRepo & IngredientsRepo, InternalServerError, Seq[PublicationRequestSummary]] = {

  def toPublicationRequest(req: PublicationRequest) = req match // some shitty code... I will fix this later
    case RecipePublicationRequest(id, entityId, createdAt, updatedAt, _, _) =>
      ZIO.serviceWithZIO[RecipesRepo](_.getRecipe(entityId))
        .someOrFail(InternalServerError())
        .map(recipe => PublicationRequestSummary(id, Recipe, recipe.name, createdAt))
    case IngredientPublicationRequest(id, entityId, createdAt, updatedAt, _, _) =>
      ZIO.serviceWithZIO[IngredientsRepo](_.get(entityId))
        .someOrFail(InternalServerError())
        .map(recipe => PublicationRequestSummary(id, Ingredient, recipe.name, createdAt))

  paginationParams match
    case PaginationParams(count, offset) => {
      for
        pendingIngredientReqs <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getAllPending)
          .flatMap { reqs =>
            ZIO.collectAll(reqs.map(req => toPublicationRequest(req.toDomain)))
          }
        pendingRecipeReqs <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllPending)
          .flatMap { reqs =>
            ZIO.collectAll(reqs.map(req => toPublicationRequest(req.toDomain)))
          }
      yield (pendingRecipeReqs ++ pendingIngredientReqs)
        .sortWith((sum1, sum2) => sum1.createdAt.toEpochSecond < sum2.createdAt.toEpochSecond)
        .slice(offset, offset + count)
    }.mapError(_ => InternalServerError())
}


