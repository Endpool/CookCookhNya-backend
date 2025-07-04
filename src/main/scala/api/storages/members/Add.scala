package api.storages.members

import api.{
  AppEnv,
  zSecuredServerLogic,
  handleFailedSqlQuery,
  toStorageNotFound,
  toUserNotFound,
}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant, userNotFoundVariant}
import db.DbError.*
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.{ErrorResponse, InternalServerError, StorageError, StorageId, UserError, UserId}

import io.circe.{Encoder, Json}
import io.circe.syntax.*
import sttp.model.StatusCode
import sttp.tapir.json.circe.*
import sttp.tapir.ValidationError
import sttp.tapir.ztapir.*
import zio.{IO, ZIO}

private val add: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
    .put
    .in(path[UserId]("memberId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, userNotFoundVariant, storageNotFoundVariant))
    .zSecuredServerLogic(addHandler)

private def addHandler(userId: UserId)(storageId: StorageId, memberId: UserId):
  ZIO[StorageMembersRepo & StoragesRepo,
      InternalServerError | UserError.NotFound | StorageError.NotFound,
      Unit] = {
  for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo] {
      _.getById(storageId)
    }
    ownerId <- ZIO.fromOption(mStorage)
      .orElseFail[StorageError.NotFound](StorageError.NotFound(storageId.toString))
      .map(_.ownerId)
    _ <- ZIO.unless(ownerId == memberId) {
      ZIO.serviceWithZIO[StorageMembersRepo] {
        _.addMemberToStorageById(storageId, memberId)
      }
    }
  yield ()
}.mapError {
  case e: StorageError.NotFound => e
  case _: DbNotRespondingError => InternalServerError()
  case e: FailedDbQuery => handleFailedSqlQuery(e)
    .flatMap(toUserNotFound)
    .getOrElse(InternalServerError())
}
