package api.storages.members

import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant, userNotFoundVariant}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import db.DbError
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.{InternalServerError, StorageId, UserId}
import domain.StorageError.NotFound

import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import zio.ZIO

private type RemoveEnv = StorageMembersRepo & StoragesRepo

private val remove: ZServerEndpoint[RemoveEnv, Any] =
  storagesMembersEndpoint
  .delete
  .in(path[UserId]("memberId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(removeHandler)

// TODO this endpoint ignores auth
private def removeHandler(storageId: StorageId, memberId: UserId):
  ZIO[AuthenticatedUser & RemoveEnv, InternalServerError | NotFound, Unit] =
  {
    for
      mStorage <- ZIO.serviceWithZIO[StoragesRepo](_.getById(storageId))
      _ <- ZIO.fromOption(mStorage).orElseFail(NotFound(storageId.toString))
      _ <- ZIO.serviceWithZIO[StorageMembersRepo] {
        _.removeMemberFromStorageById(storageId, memberId)
      }
    yield ()
  }.catchAll {
    case _: DbError  => ZIO.fail(InternalServerError())
    case e: NotFound => ZIO.fail(e)
  }
