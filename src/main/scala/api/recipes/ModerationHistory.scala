package api.recipes

import domain.{InternalServerError, RecipeNotFound, RecipeId}
import api.EndpointErrorVariants.{serverErrorVariant, recipeNotFoundVariant}
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import db.tables.publication.DbPublicationRequestStatus
import db.repositories.{RecipePublicationRequestsRepo, RecipesRepo}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import java.time.OffsetDateTime

final case class ModerationHistoryResponse(
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: DbPublicationRequestStatus,
  reason: Option[String]
)

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
      .map(dbReq => ModerationHistoryResponse(dbReq.createdAt, dbReq.updatedAt, dbReq.status, dbReq.reason))
      .sortBy(_.updatedAt)

  yield res