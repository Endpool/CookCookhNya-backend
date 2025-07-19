package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.{PaginationParams, paginate}
import api.EndpointErrorVariants.serverErrorVariant
import api.moderation.pubrequests.PublicationRequestTypeResp.*
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import domain.{InternalServerError, PublicationRequestId}

import io.circe.generic.auto.*
import java.time.OffsetDateTime
import sttp.model.StatusCode.NoContent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final case class PublicationRequestSummary(
                                            id: PublicationRequestId,
                                            requestType: PublicationRequestTypeResp,
                                            entityName: String,
                                            createdAt: OffsetDateTime
)

private type GetSomePendingEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo

private val getSomePending: ZServerEndpoint[GetSomePendingEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(PaginationParams.query)
    .out(jsonBody[Seq[PublicationRequestSummary]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(getSomePendingHandler)

private def getSomePendingHandler(paginationParams: PaginationParams):
  ZIO[AuthenticatedUser & GetSomePendingEnv, InternalServerError, Seq[PublicationRequestSummary]] =
  for
    pendingIngredientReqs <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_
      .getPendingRequestsWithIngredients
      .orElseFail(InternalServerError())
      .map(_.map { case (req, ingredient) =>
        PublicationRequestSummary(req.id, Ingredient, ingredient.name, req.createdAt)
      })
    )
    pendingRecipeReqs <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .getPendingRequestsWithRecipes
      .orElseFail(InternalServerError())
      .map(_.map { case (req, recipe) =>
        PublicationRequestSummary(req.id, Recipe, recipe.name, req.createdAt)
      })
    )
  yield (pendingRecipeReqs ++ pendingIngredientReqs)
    .sortBy(_.createdAt.toEpochSecond)
    .paginate(paginationParams)
