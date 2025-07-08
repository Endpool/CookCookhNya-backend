package api.invitations

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{invalidInvitationHashVariant, serverErrorVariant, storageAccessForbiddenVariant, storageNotFoundVariant}
import db.repositories.{InvitationsRepo, StorageMembersRepo}
import domain.{InternalServerError, InvalidInvitationHash}

import sttp.tapir.ztapir.*
import zio.ZIO

private type ActivateEnv = InvitationsRepo & StorageMembersRepo
val activate: ZServerEndpoint[ActivateEnv, Any] = invitationEndpoint
  .post
  .in(path[String]("invitationHash") / "activate")
  .errorOut(oneOf(invalidInvitationHashVariant, serverErrorVariant))
  .zSecuredServerLogic(activateHandler)

def activateHandler(invitationHash: String):
  ZIO[AuthenticatedUser & ActivateEnv, InvalidInvitationHash | InternalServerError, Unit] =
  ZIO.serviceWithZIO[InvitationsRepo](_.activate(invitationHash)).debug
    .mapError {
      case e: InvalidInvitationHash => e
      case _ => InternalServerError()
    }