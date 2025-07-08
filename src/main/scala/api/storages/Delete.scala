package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageAccessForbiddenVariant}
import db.DbError
import db.repositories.StoragesRepo
import domain.{StorageId, UserId, InternalServerError}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO
import domain.StorageError

private type DeleteEnv = StoragesRepo

private val delete: ZServerEndpoint[DeleteEnv, Any] =
  storagesEndpoint
  .delete
  .in(path[StorageId]("storageId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, storageAccessForbiddenVariant))
  .zSecuredServerLogic(deleteHandler)

private def deleteHandler(storageId: StorageId):
  ZIO[AuthenticatedUser & DeleteEnv, InternalServerError | StorageError.AccessForbidden, Unit] = for
  userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
  repo <- ZIO.service[StoragesRepo]
  mStorage <- repo.getById(storageId).orElseFail(InternalServerError())
  _ <- mStorage match
    case None => ZIO.unit
    case Some(storage) if storage.ownerId == userId =>
      repo.removeById(storageId).orElseFail(InternalServerError())
    case _ => ZIO.fail[StorageError.AccessForbidden](StorageError.AccessForbidden(storageId.toString))
yield ()
