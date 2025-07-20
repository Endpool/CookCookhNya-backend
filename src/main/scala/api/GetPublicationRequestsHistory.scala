package api

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.PaginationParams
import api.moderation.ModerationHistoryResponse
import db.repositories.{RecipePublicationRequestsRepo, IngredientPublicationRequestsRepo}
import domain.InternalServerError

import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetPublicationRequestsHistoryEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo

val getPublicationRequestsHistory: ZServerEndpoint[GetPublicationRequestsHistoryEnv, Any] =
  endpoint
    .get
    .in("publication-requests" / PaginationParams.query)
    .errorOut(oneOf(serverErrorVariant))
    .out(jsonBody[List[ModerationHistoryResponse]])
    .zSecuredServerLogic(getPublicationRequestsHistoryHandler)

def getPublicationRequestsHistoryHandler(paginationParams: PaginationParams):
  ZIO[
    AuthenticatedUser & GetPublicationRequestsHistoryEnv,
    InternalServerError,
    List[ModerationHistoryResponse]
  ] =
  for
    dbRecipeRequests     <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .getAllCreatedBy
      .orElseFail(InternalServerError())
    )
    dbIngredientRequests <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_
      .getAllCreatedBy
      .orElseFail(InternalServerError())
    )
    recipeRequests = dbRecipeRequests
      .map(
        dbReq => ModerationHistoryResponse(
          dbReq.createdAt,
          dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
          dbReq.reason
        )
      )
    ingredientRequests = dbIngredientRequests
      .map(
        dbReq => ModerationHistoryResponse(
          dbReq.createdAt,
          dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
          dbReq.reason
        )
      )
  yield (recipeRequests ++ ingredientRequests)
    .sortBy(_.updatedAt)
