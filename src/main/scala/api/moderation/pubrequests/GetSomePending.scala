package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.{PaginationParams, paginate}
import api.moderation.pubrequests.PublicationRequestType.*
import db.repositories.{
  IngredientPublicationRequestsRepo,
  IngredientsRepo,
  RecipePublicationRequestsRepo,
  RecipesRepo
}
import domain.{IngredientPublicationRequest, InternalServerError, RecipePublicationRequest}

import io.circe.generic.auto.*
import java.time.OffsetDateTime
import java.util.UUID
import sttp.model.StatusCode.NoContent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final case class PublicationRequestSummary(
  id: UUID,
  requestType: PublicationRequestType,
  entityName: String,
  createdAt: OffsetDateTime
)

private type GetSomePendingEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo
  & RecipesRepo
  & IngredientsRepo

private val getSomePending: ZServerEndpoint[GetSomePendingEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(PaginationParams.query)
    .out(statusCode(NoContent))
    .out(jsonBody[Seq[PublicationRequestSummary]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(getSomePendingHandler)

private def getSomePendingHandler(paginationParams: PaginationParams):
  ZIO[AuthenticatedUser & GetSomePendingEnv, InternalServerError, Seq[PublicationRequestSummary]] =

  def toRecipePublicationRequest(req: RecipePublicationRequest) =
    ZIO.serviceWithZIO[RecipesRepo](_
      .getRecipe(req.recipeId)
      .someOrFail(InternalServerError())
      .map(recipe => PublicationRequestSummary(req.id, Recipe, recipe.name, req.createdAt))
    )

  def toIngredientPublicationRequest(req: IngredientPublicationRequest) =
    ZIO.serviceWithZIO[IngredientsRepo](_
      .get(req.ingredientId)
      .someOrFail(InternalServerError())
      .map(ingredient => PublicationRequestSummary(req.id, Ingredient, ingredient.name, req.createdAt))
    )

  for
    pendingRecipeReqs <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllPending)
      .flatMap { reqs =>
        ZIO.collectAll(reqs.map(req => toRecipePublicationRequest(req.toDomain)))
      }.orElseFail(InternalServerError())
    pendingIngredientReqs <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getAllPending)
      .flatMap { reqs =>
        ZIO.collectAll(reqs.map(req => toIngredientPublicationRequest(req.toDomain)))
      }.orElseFail(InternalServerError())
  yield (pendingRecipeReqs ++ pendingIngredientReqs)
    .sortBy(_.createdAt.toEpochSecond)
    .paginate(paginationParams)
