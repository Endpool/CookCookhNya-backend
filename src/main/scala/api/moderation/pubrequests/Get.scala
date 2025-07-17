package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.moderation.pubrequests.PublicationRequestType.*
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.{
  IngredientPublicationRequestsRepo,
  IngredientsRepo,
  RecipePublicationRequestsRepo,
  RecipesRepo
}
import domain.{
  IngredientPublicationRequest,
  InternalServerError,
  PublicationRequestNotFound,
  PublicationRequestStatus,
  RecipePublicationRequest,
}

import io.circe.generic.auto.*
import java.time.OffsetDateTime
import java.util.UUID
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final case class PublicationRequestResp(
  id: UUID,
  requestType: PublicationRequestType,
  entityId: UUID,
  entityName: String,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  comment: String,
  status: PublicationRequestStatus
)

private type GetReqEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo
  & RecipesRepo
  & IngredientsRepo

private type PublicationRequest = RecipePublicationRequest | IngredientPublicationRequest

private val getRequest: ZServerEndpoint[GetReqEnv, Any] =
  publicationRequestEndpoint
    .get
    .in(path[UUID]("id"))
    .out(jsonBody[PublicationRequestResp])
    .errorOut(oneOf(serverErrorVariant, publicationRequestNotFound))
    .zSecuredServerLogic(getRequestHandler)

private def getRequestHandler(reqId: UUID):
  ZIO[AuthenticatedUser & GetReqEnv,
      InternalServerError | PublicationRequestNotFound,
      PublicationRequestResp] =
  def getIngredientRequest =
    ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.get(reqId))
      .flatMap {
        _.map { dbEntity =>
          val IngredientPublicationRequest(id, ingredientId, createdAt, updatedAt, status, comment) = dbEntity.toDomain
          ZIO.serviceWithZIO[IngredientsRepo] {
            _.get(ingredientId).some.map { ingredient =>
              PublicationRequestResp(
                reqId,
                Ingredient,
                ingredientId,
                ingredient.name,
                createdAt,
                updatedAt,
                comment,
                status
              )
            }
          }
        }.getOrElse(ZIO.fail(PublicationRequestNotFound(reqId)))
      }

  ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.get(reqId))
    .flatMap {
      _.map { dbEntity =>
        val RecipePublicationRequest(id, recipeId, createdAt, updatedAt, status, comment) = dbEntity.toDomain
        ZIO.serviceWithZIO[RecipesRepo] {
          _.getRecipe(recipeId).some.map { recipe =>
            PublicationRequestResp(
              reqId,
              Recipe,
              recipeId,
              recipe.name,
              createdAt,
              updatedAt,
              comment,
              status
            )
          }
        }
      }.getOrElse(getIngredientRequest)
    }.mapError {
      case _: (Option[_] | FailedDbQuery) => PublicationRequestNotFound(reqId)
      case _: DbNotRespondingError => InternalServerError()
      case x: PublicationRequestNotFound => x
    }
