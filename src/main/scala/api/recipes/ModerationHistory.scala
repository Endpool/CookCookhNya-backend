package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant}
import api.PublicationRequestStatusResp
import api.moderation.ModerationHistoryResponse
import db.repositories.{RecipePublicationRequestsRepo, RecipesRepo}
import domain.{InternalServerError, RecipeId, RecipeNotFound}

import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO


private type ModerationHistoryEnv = RecipePublicationRequestsRepo & RecipesRepo
val moderationHistory: ZServerEndpoint[ModerationHistoryEnv, Any] =
  recipesEndpoint
    .get
    .in(path[RecipeId]("recipe-id") / "moderation-history")
    .errorOut(oneOf(serverErrorVariant, recipeNotFoundVariant))
    .out(jsonBody[List[ModerationHistoryResponse]])
    .zSecuredServerLogic(moderationHistoryHandler)

def moderationHistoryHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & ModerationHistoryEnv, InternalServerError | RecipeNotFound, List[ModerationHistoryResponse]] =
  for
    isUserOwner <- ZIO.serviceWithZIO[RecipesRepo](_.isUserOwner(recipeId))
      .orElseFail(InternalServerError())
    _ <- ZIO.fail(RecipeNotFound(recipeId)).unless(isUserOwner)

    dbRequests <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllByRecipeId(recipeId))
      .orElseFail(InternalServerError())
    res = dbRequests
      .map(
        dbReq => ModerationHistoryResponse(
          dbReq.createdAt, dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
          dbReq.reason
        )
      )
      .sortBy(_.updatedAt)

  yield res