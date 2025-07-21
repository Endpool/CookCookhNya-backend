package api

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.common.search.PaginationParams
import api.moderation.ModerationHistoryResponse
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import db.tables.{DbIngredient, DbRecipe}
import db.tables.publication.{DbIngredientPublicationRequest, DbRecipePublicationRequest}
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
    recipeRequests = dbRecipeRequests.map {
      case (dbRecipeReq: DbRecipePublicationRequest, recipe: DbRecipe) => ModerationHistoryResponse(
        recipe.name,
        "recipe",
        dbRecipeReq.createdAt,
        dbRecipeReq.updatedAt,
        PublicationRequestStatusResp.fromDomain(dbRecipeReq.status.toDomain(dbRecipeReq.reason)),
        dbRecipeReq.reason
      )
    }
    
    dbIngredientRequests <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_
      .getAllCreatedBy
      .orElseFail(InternalServerError())
    )
    ingredientRequests = dbIngredientRequests.map {
        case (dbRecipeReq: DbIngredientPublicationRequest, ingredient: DbIngredient) => ModerationHistoryResponse(
          ingredient.name,
          "ingredient",
          dbRecipeReq.createdAt,
          dbRecipeReq.updatedAt,
          PublicationRequestStatusResp.fromDomain(dbRecipeReq.status.toDomain(dbRecipeReq.reason)),
          dbRecipeReq.reason
        )
      }
  yield (recipeRequests ++ ingredientRequests)
    .sortBy(_.updatedAt)
