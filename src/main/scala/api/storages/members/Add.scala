package api.storages.members

import api.{AppEnv, handleFailedSqlQuery, failIfStorageNotFound, failIfUserNotFound}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant, userNotFoundVariant}
import api.zSecuredServerLogic
import db.DbError
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.{ErrorResponse, InternalServerError, StorageError, StorageId, UserError, UserId}
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import zio.{IO, ZIO}
import sttp.tapir.ValidationError
import io.circe.{Encoder, Json}
import io.circe.syntax.*

private val add: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .put
  .in(path[UserId]("memberId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, userNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(addHandler)

private def addHandler(userId: UserId)(storageId: StorageId, memberId: UserId):
  ZIO[StorageMembersRepo & StoragesRepo, InternalServerError | UserError.NotFound | StorageError.NotFound, Unit] =
  {
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
  }.catchAll {
    case e: StorageError.NotFound => ZIO.fail(e)
    case DbError.DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: DbError.FailedDbQuery => {
        for {
          missingEntry <- handleFailedSqlQuery(e)
          (keyName, keyValue, _) = missingEntry
          _ <- failIfUserNotFound(keyName, keyValue)
          _ <- ZIO.fail(InternalServerError())
        } yield ()
      }: IO[InternalServerError | UserError.NotFound | StorageError.NotFound, Unit]
  }
