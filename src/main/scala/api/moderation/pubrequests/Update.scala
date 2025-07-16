package api.moderation.pubrequests

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{publicationRequestNotFound, serverErrorVariant}
import api.moderation.pubrequests.PublicationRequestType.*
import db.DbError
import db.repositories.{IngredientPublicationRequestsRepo, RecipePublicationRequestsRepo}
import db.tables.publication.DbPublicationRequestStatus
import domain.{InternalServerError, PublicationRequestNotFound}
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO
import sttp.model.StatusCode.NoContent

import java.util.UUID

private type UpdateReqEnv
  = RecipePublicationRequestsRepo
  & IngredientPublicationRequestsRepo


private val updatePublicationRequest: ZServerEndpoint[UpdateReqEnv, Any] =
  publicationRequestEndpoint
    .patch
    .in(query[UUID]("id"))
    .in(jsonBody[PublicationRequestUpdate])
    .out(statusCode(NoContent))
    .errorOut(oneOf(publicationRequestNotFound, serverErrorVariant))
    .zSecuredServerLogic(updatePublicationRequestHandler)

private def updatePublicationRequestHandler(id: UUID, reqBody: PublicationRequestUpdate):
  ZIO[AuthenticatedUser & UpdateReqEnv, InternalServerError | PublicationRequestNotFound, Unit] =

  reqBody match
    case PublicationRequestUpdate(comment, status) =>
      val (_, dbStatus) = DbPublicationRequestStatus.fromDomain(status)
      ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.update(id, comment, dbStatus))
        .catchSome {
          case _: PublicationRequestNotFound =>
            ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_.update(id, comment, dbStatus))
        }.mapError {
          case x: PublicationRequestNotFound => x
          case _: DbError                    => InternalServerError()
        }
