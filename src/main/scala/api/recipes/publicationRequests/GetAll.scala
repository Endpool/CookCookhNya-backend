package api.recipes.publicationRequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant}
import api.PublicationRequestStatusResp
import db.repositories.{RecipePublicationRequestsRepo, RecipesRepo}
import domain.{InternalServerError, RecipeId, RecipeNotFound}

import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import java.time.OffsetDateTime
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class RecipeModerationHistoryResponse(
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatusResp,
)

private type GetAllEnv = RecipePublicationRequestsRepo & RecipesRepo

val getAll: ZServerEndpoint[GetAllEnv, Any] =
  recipesPublicationRequestsEndpoint
    .get
    .out(jsonBody[List[RecipeModerationHistoryResponse]])
    .errorOut(oneOf(serverErrorVariant, recipeNotFoundVariant))
    .zSecuredServerLogic(getAllHandler)

private def getAllHandler(recipeId: RecipeId):
  ZIO[
    AuthenticatedUser & GetAllEnv,
    InternalServerError | RecipeNotFound,
    List[RecipeModerationHistoryResponse]
  ] =
  for
    userIsOwner <- ZIO.serviceWithZIO[RecipesRepo](_.isUserOwner(recipeId))
      .orElseFail(InternalServerError())
    _ <- ZIO.fail(RecipeNotFound(recipeId))
      .unless(userIsOwner)

    dbRequests <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllByRecipeId(recipeId))
      .orElseFail(InternalServerError())
    res = dbRequests
      .map(
        dbReq => RecipeModerationHistoryResponse(
          dbReq.createdAt, dbReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbReq.status.toDomain(dbReq.reason)),
        )
      )
      .sortBy(_.updatedAt)
  yield res
