package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.moderation.pubrequests.PublicationRequestType.*
import db.DbError
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import db.tables.publication.DbPublicationRequestStatus
import domain.{PublicationRequestStatus, InternalServerError, PublicationRequestNotFound}

import io.circe.generic.auto.*
import java.util.UUID
import sttp.model.StatusCode.NoContent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO
import io.circe.Encoder

enum PublicationRequestStatusReq:
  case Pending
  case Accepted
  case Rejected(reason: String)
  def toDomain: PublicationRequestStatus = this match
    case Pending          => PublicationRequestStatus.Pending
    case Accepted         => PublicationRequestStatus.Accepted
    case Rejected(reason) => PublicationRequestStatus.Rejected(reason)

final case class UpdatePublicationRequestReqBody(status: PublicationRequestStatusReq)
object UpdatePublicationRequestReqBody:
  given Encoder[UpdatePublicationRequestReqBody] = Encoder { req => req.status match
    case _ => ???

  }


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
  val status = reqBody.status.toDomain
  ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.update(id, status))
    .catchSome {
      case _: PublicationRequestNotFound =>
        ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.update(id, status))
    }.mapError {
      case x: PublicationRequestNotFound => x
      case _: DbError                    => InternalServerError()
    }
