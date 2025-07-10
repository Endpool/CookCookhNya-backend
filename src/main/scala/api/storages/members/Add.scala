package api.storages.members

import api.{
  handleFailedSqlQuery,
  toStorageNotFound,
  toUserNotFound,
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant, userNotFoundVariant}
import db.DbError.*
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.{ErrorResponse, InternalServerError, StorageNotFound, StorageId, UserNotFound, UserId}

import io.circe.{Encoder, Json}
import io.circe.syntax.*
import sttp.model.StatusCode
import sttp.tapir.json.circe.*
import sttp.tapir.ValidationError
import sttp.tapir.ztapir.*
import zio.{IO, ZIO}

private type AddEnv = StorageMembersRepo & StoragesRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  storagesMembersEndpoint
    .put
    .in(path[UserId]("memberId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, userNotFoundVariant, storageNotFoundVariant))
    .zSecuredServerLogic(addHandler)

private def addHandler(storageId: StorageId, memberId: UserId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError | UserNotFound | StorageNotFound,
      Unit] = {
  for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo] {
      _.getById(storageId)
    }
    ownerId <- ZIO.fromOption(mStorage)
      .orElseFail(StorageNotFound(storageId.toString))
      .map(_.ownerId)
    _ <- ZIO.unless(ownerId == memberId) {
      ZIO.serviceWithZIO[StorageMembersRepo] {
        _.addMemberToStorageById(storageId, memberId)
      }
    }
  yield ()
}.mapError {
  case e: StorageNotFound => e
  case _: DbNotRespondingError => InternalServerError()
  case e: FailedDbQuery => handleFailedSqlQuery(e)
    .flatMap(toUserNotFound)
    .getOrElse(InternalServerError())
}
