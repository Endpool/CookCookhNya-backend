package api.invitations

import api.handleFailedSqlQuery
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{serverErrorVariant, storageAccessForbiddenVariant, storageNotFoundVariant}
import db.{DbError, handleDbError}
import db.repositories.{InvitationsRepo, StorageMembersRepo, StoragesRepo}
import domain.{InternalServerError, InvalidInvitationHash, StorageNotFound, StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val create = invitationEndpoint
  .post
  .in("to" / path[StorageId])
  .out(plainBody[String])
  .errorOut(oneOf(storageNotFoundVariant, storageAccessForbiddenVariant, serverErrorVariant))
  .zSecuredServerLogic(createHandler)

private type CreateEnv = InvitationsRepo & StorageMembersRepo  & StoragesRepo
def createHandler(storageId: StorageId): ZIO[AuthenticatedUser & CreateEnv, InternalServerError | StorageNotFound, String] =
  for
    storageExists <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      .map(_.isDefined)
      .mapError(_ => InternalServerError())
    _ <- ZIO.fail(StorageNotFound(storageId.toString)).unless(storageExists)

    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    isMemberOrOwner <- ZIO.serviceWithZIO[StorageMembersRepo](_.checkForMembership(userId, storageId))
      .mapError(_ => InternalServerError())
    _ <- ZIO.fail(StorageNotFound(storageId.toString)).unless(isMemberOrOwner)
    inviteHash <- ZIO.serviceWithZIO[InvitationsRepo](_.create(storageId))
      .mapError(_ => InternalServerError())
  yield inviteHash