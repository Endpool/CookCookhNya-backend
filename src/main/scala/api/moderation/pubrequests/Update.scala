package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.moderation.pubrequests.PublicationRequestType.*
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import domain.{PublicationRequestStatus, InternalServerError, PublicationRequestNotFound}

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
  status: PublicationRequestStatusReq,
  reason: Option[String],
):
  def getDomainStatus: PublicationRequestStatus = status match
    case PublicationRequestStatusReq.Pending  => PublicationRequestStatus.Pending
    case PublicationRequestStatusReq.Accepted => PublicationRequestStatus.Accepted
    case PublicationRequestStatusReq.Rejected => PublicationRequestStatus.Rejected(reason)

private type UpdateReqEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo

private val updatePublicationRequest: ZServerEndpoint[UpdateReqEnv, Any] =
  publicationRequestEndpoint
    .patch
    .in(query[UUID]("id"))
    .in(jsonBody[UpdatePublicationRequestReqBody])
    .out(statusCode(NoContent))
    .errorOut(oneOf(publicationRequestNotFound, serverErrorVariant))
    .zSecuredServerLogic(updatePublicationRequestHandler)

private def updatePublicationRequestHandler(id: UUID, reqBody: UpdatePublicationRequestReqBody):
  ZIO[AuthenticatedUser & UpdateReqEnv, InternalServerError | PublicationRequestNotFound, Unit] =
  val status = reqBody.getDomainStatus
  for
    rowsUpdated <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .updateStatus(id, status)
      .orElseFail(InternalServerError())
    )
        // case _: PublicationRequestNotFound =>
        //   ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.updateStatus(id, status))
  yield ()
