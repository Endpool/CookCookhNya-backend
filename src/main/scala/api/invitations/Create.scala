package api.invitations

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{serverErrorVariant, storageAccessForbiddenVariant, storageNotFoundVariant}
import db.repositories.{InvitationsRepo, StorageMembersRepo, StoragesRepo}
import domain.{InternalServerError, StorageNotFound, StorageId}

import sttp.tapir.ztapir.*
import zio.ZIO

private type CreateEnv = InvitationsRepo & StorageMembersRepo  & StoragesRepo
val create: ZServerEndpoint[CreateEnv, Any] = invitationEndpoint
  .post
  .in("to" / path[StorageId]("storageId"))
  .out(plainBody[String])
  .errorOut(oneOf(storageNotFoundVariant, storageAccessForbiddenVariant, serverErrorVariant))
  .zSecuredServerLogic(createHandler)

def createHandler(storageId: StorageId): ZIO[AuthenticatedUser & CreateEnv, InternalServerError | StorageNotFound, String] =
  for
    storageExists <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .map(_.isDefined)
      .orElseFail(InternalServerError())
    _ <- ZIO.fail(StorageNotFound(storageId.toString)).unless(storageExists)

    inviteHash <- ZIO.serviceWithZIO[InvitationsRepo](_.create(storageId))
      .orElseFail(InternalServerError())
  yield inviteHash
