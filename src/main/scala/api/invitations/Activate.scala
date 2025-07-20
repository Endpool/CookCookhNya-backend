package api.invitations

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{invalidInvitationHashVariant, serverErrorVariant}
import api.storages.StorageSummaryResp
import db.repositories.{InvitationsRepo, StorageMembersRepo}
import domain.{InternalServerError, InvalidInvitationHash}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type ActivateEnv = InvitationsRepo & StorageMembersRepo
val activate: ZServerEndpoint[ActivateEnv, Any] = invitationEndpoint
  .post
  .in(path[String]("invitationHash") / "activate")
  .out(jsonBody[StorageSummaryResp]) 
  .errorOut(oneOf(invalidInvitationHashVariant, serverErrorVariant))
  .zSecuredServerLogic(activateHandler)

def activateHandler(invitationHash: String):
  ZIO[AuthenticatedUser & ActivateEnv, InvalidInvitationHash | InternalServerError, StorageSummaryResp] =
  ZIO.serviceWithZIO[InvitationsRepo](_.activate(invitationHash))
    .map{case (storageId, storageName) => StorageSummaryResp(storageId, storageName)}
    .mapError {
      case e: InvalidInvitationHash => e
      case _ => InternalServerError()
    }
