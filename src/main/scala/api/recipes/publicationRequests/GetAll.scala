package api.recipes.publicationRequests

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

private type GetAllEnv = RecipePublicationRequestsRepo & RecipesRepo

val getAll: ZServerEndpoint[GetAllEnv, Any] =
  recipesPublicationRequestsEndpoint
    .get
    .out(jsonBody[List[ModerationHistoryResponse]])
    .errorOut(oneOf(serverErrorVariant, recipeNotFoundVariant))
    .zSecuredServerLogic(moderationHistoryHandler)

def moderationHistoryHandler(recipeId: RecipeId):
  ZIO[
    AuthenticatedUser & GetAllEnv,
    InternalServerError | RecipeNotFound,
    List[ModerationHistoryResponse]
  ] =
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
