package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.{PaginationParams, paginate}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.moderation.pubrequests.PublicationRequestType.*
import db.DbError
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import domain.{IngredientPublicationRequest, InternalServerError, PublicationRequestNotFound, RecipePublicationRequest}
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

private val getSomePending: ZServerEndpoint[GetSomePendingEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(PaginationParams.query)
    .out(statusCode(NoContent))
    .out(jsonBody[Vector[PublicationRequestSummary]])
    .errorOut(oneOf(serverErrorVariant, publicationRequestNotFound))
    .zSecuredServerLogic(getSomePendingHandler)

private def getSomePendingHandler(paginationParams: PaginationParams):
  ZIO[AuthenticatedUser & GetSomePendingEnv, InternalServerError | PublicationRequestNotFound, Vector[PublicationRequestSummary]] =
  {
    for
      pendingIngredientReqs <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getAllPendingIds)
        .flatMap { ids =>
          ZIO.collectAll {
            ids.map { id =>
              ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getWithIngredient(id))
                .someOrFail(PublicationRequestNotFound(id))
                .map {
                  case (dbPubReq, dbIngredient) =>
                    PublicationRequestSummary(id, Ingredient, dbIngredient.name, dbPubReq.createdAt)
                }
            }
          }
        }
      pendingRecipeReqs <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllPendingIds)
        .flatMap { ids =>
          ZIO.collectAll {
            ids.map { id =>
              ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getWithRecipe(id))
                .someOrFail(PublicationRequestNotFound(id))
                .map {
                  case (dbPubReq, dbRecipe) =>
                    PublicationRequestSummary(id, Recipe, dbRecipe.name, dbPubReq.createdAt)
                }
            }
          }
        }
    yield (pendingRecipeReqs ++ pendingIngredientReqs)
      .sortBy(_.createdAt.toEpochSecond)
      .paginate(paginationParams)
  }.mapError {
    case _: DbError                    => InternalServerError()
    case x: PublicationRequestNotFound => x
  }