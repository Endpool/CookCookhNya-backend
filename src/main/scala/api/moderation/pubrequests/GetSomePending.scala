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
  ZIO[AuthenticatedUser & GetSomePendingEnv, InternalServerError, Seq[PublicationRequestSummary]] =

  def toPublicationRequest(req: PublicationRequest) = req match // some shitty code... I will fix this later
    case RecipePublicationRequest(id, entityId, createdAt, updatedAt, _, _) =>
      ZIO.serviceWithZIO[RecipesRepo](_.getRecipe(entityId))
        .someOrFail(InternalServerError())
        .map(recipe => PublicationRequestSummary(id, Recipe, recipe.name, createdAt))
    case IngredientPublicationRequest(id, entityId, createdAt, updatedAt, _, _) =>
      ZIO.serviceWithZIO[IngredientsRepo](_.get(entityId))
        .someOrFail(InternalServerError())
        .map(ingredient => PublicationRequestSummary(id, Ingredient, ingredient.name, createdAt))

  val PaginationParams(count, offset) = paginationParams
  for
    pendingIngredientReqs <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getAllPending)
      .flatMap { reqs =>
        ZIO.collectAll(reqs.map(req => toPublicationRequest(req.toDomain)))
      }.orElseFail(InternalServerError())
    pendingRecipeReqs <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllPending)
      .flatMap { reqs =>
        ZIO.collectAll(reqs.map(req => toPublicationRequest(req.toDomain)))
      }.orElseFail(InternalServerError())
  yield (pendingRecipeReqs ++ pendingIngredientReqs)
    .sortWith(_.createdAt.toEpochSecond < _.createdAt.toEpochSecond)
    .slice(offset, offset + count)
