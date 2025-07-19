package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.PublicationRequestStatusResp
import api.moderation.pubrequests.PublicationRequestTypeResp.*
import db.DbError
import domain.{IngredientPublicationRequest, InternalServerError, PublicationRequestId, PublicationRequestNotFound, RecipePublicationRequest}
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import io.circe.generic.auto.*

import java.time.OffsetDateTime
import java.util.UUID
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final case class PublicationRequestResp(
   id: UUID,
   requestType: PublicationRequestTypeResp,
   entityId: UUID,
   entityName: String,
   createdAt: OffsetDateTime,
   updatedAt: OffsetDateTime,
   status: PublicationRequestStatusResp
)

private type GetReqEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo

private val getRequest: ZServerEndpoint[GetReqEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(path[UUID]("id"))
    .out(jsonBody[PublicationRequestResp])
    .errorOut(oneOf(serverErrorVariant, publicationRequestNotFound))
    .zSecuredServerLogic(getRequestHandler)

private def getRequestHandler(reqId: PublicationRequestId):
  ZIO[AuthenticatedUser & GetReqEnv,
      InternalServerError | PublicationRequestNotFound,
      PublicationRequestResp] =

  def getIngredientRequest =
    ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getWithIngredient(reqId))
      .someOrFail(PublicationRequestNotFound(reqId))
      .map {
        case (dbReq, dbIngredient) => dbReq.toDomain match
          case IngredientPublicationRequest(id, ingredientId, createdAt, updatedAt, status) =>
            PublicationRequestResp(
              reqId,
              Ingredient,
              ingredientId,
              dbIngredient.name,
              createdAt,
              updatedAt,
              PublicationRequestStatusResp.fromDomain(status)
            )
      }

  def getRecipeRequest =
    ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getWithRecipe(reqId))
      .someOrFail(PublicationRequestNotFound(reqId))
      .map {
        case (dbReq, dbRecipe) => dbReq.toDomain match
          case RecipePublicationRequest(id, ingredientId, createdAt, updatedAt, status) =>
            PublicationRequestResp(
              reqId,
              Ingredient,
              ingredientId,
              dbRecipe.name,
              createdAt,
              updatedAt,
              PublicationRequestStatusResp.fromDomain(status)
            )
      }

  getIngredientRequest.orElse(getRecipeRequest).mapError {
    case _: DbError => InternalServerError()
    case x: PublicationRequestNotFound => x
  }
