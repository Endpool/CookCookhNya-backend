package api.moderation

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.PublicationRequestStatusResp
import api.common.search.PaginationParams
import db.repositories.{RecipePublicationRequestsRepo, IngredientPublicationRequestsRepo}
import domain.InternalServerError

import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO


private type FullHistoryEnv = RecipePublicationRequestsRepo & IngredientPublicationRequestsRepo
val fullHistory: ZServerEndpoint[FullHistoryEnv, Any] =
  endpoint
    .get
    .in("publication-requests" / PaginationParams.query)
    .errorOut(oneOf(serverErrorVariant))
    .out(jsonBody[List[ModerationHistoryResponse]])
    .zSecuredServerLogic(fullHistoryHandler)

def fullHistoryHandler(paginationParams: PaginationParams):
ZIO[AuthenticatedUser & FullHistoryEnv, InternalServerError, List[ModerationHistoryResponse]] =
  for
    dbRecipeRequests <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllCreatedBy)
      .orElseFail(InternalServerError())
    dbIngredientRequests <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getAllCreatedBy)
      .orElseFail(InternalServerError())
    recipeRequests = dbRecipeRequests
      .map(
        dbReq => ModerationHistoryResponse(
          dbReq.createdAt, dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
          dbReq.reason
        )
      )
    ingredientRequests = dbIngredientRequests
      .map(
        dbReq => ModerationHistoryResponse(
          dbReq.createdAt, dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
          dbReq.reason
        )
      )
    res = (recipeRequests ++ ingredientRequests).sortBy(_.updatedAt)
  yield res
