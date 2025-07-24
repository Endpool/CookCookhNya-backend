package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.PublicationRequestStatusResp
import api.moderation.pubrequests.PublicationRequestTypeResp.*
import db.repositories.{IngredientPublicationRequestsRepo, IngredientsRepo, RecipePublicationRequestsRepo, RecipesRepo}
import domain.{InternalServerError, PublicationRequestId, PublicationRequestNotFound, PublicationRequestStatus}
import io.circe.Encoder
import io.circe.generic.auto.*

import java.util.UUID
import sttp.model.StatusCode.NoContent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

enum PublicationRequestStatusReq:
  case Pending
  case Accepted
  case Rejected

final case class UpdatePublicationRequestReqBody(
  status: PublicationRequestStatusResp,
):
  def getDomainStatus: PublicationRequestStatus = status match
    case PublicationRequestStatusResp.Pending  => PublicationRequestStatus.Pending
    case PublicationRequestStatusResp.Accepted => PublicationRequestStatus.Accepted
    case PublicationRequestStatusResp.Rejected(reason) => PublicationRequestStatus.Rejected(reason)

private type UpdateReqEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo
  & IngredientsRepo
  & RecipesRepo

private val updatePublicationRequest: ZServerEndpoint[UpdateReqEnv, Any] =
  publicationRequestEndpoint
    .patch
    .in(query[UUID]("id"))
    .in(jsonBody[UpdatePublicationRequestReqBody])
    .out(statusCode(NoContent))
    .errorOut(oneOf(publicationRequestNotFound, serverErrorVariant))
    .zSecuredServerLogic(updatePublicationRequestHandler)

private def updatePublicationRequestHandler(id: PublicationRequestId, reqBody: UpdatePublicationRequestReqBody):
  ZIO[AuthenticatedUser & UpdateReqEnv, InternalServerError | PublicationRequestNotFound, Unit] =
  val status = reqBody.getDomainStatus

  def publishIngredient =
    ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.getWithIngredient(id))
      .some
      .flatMap {
        case (_, dbIngredient) =>
          val ingredientId = dbIngredient.id
          ZIO.serviceWithZIO[IngredientsRepo](_.publish(ingredientId))
      }.orElseFail(InternalServerError())

  def publishRecipe =
    ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getWithRecipe(id))
      .some
      .flatMap {
        case (_, dbRecipe) =>
          val recipeId = dbRecipe.id
          ZIO.serviceWithZIO[RecipesRepo](_.publish(recipeId))
      }.orElseFail(InternalServerError())

  for
    rowsUpdated <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .updateStatus(id, status)
      .orElseFail(InternalServerError())
    )
    rowsUpdated <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_
      .updateStatus(id, status)
      .orElseFail(InternalServerError())
    ).unless(rowsUpdated).someOrElse(false)
    _ <- ZIO.fail(PublicationRequestNotFound(id)).unless(rowsUpdated)
    _ <- ZIO.when(status == PublicationRequestStatus.Accepted) {
      publishRecipe.orElse(publishIngredient)
    }
  yield ()
