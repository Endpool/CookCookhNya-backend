package api.storages.members

import api.AppEnv
import api.EndpointErrorVariants.{userNotFoundVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.{StorageMembersRepo, StoragesRepo}
import domain.{UserError, StorageError, StorageId, UserId, DbError}
import domain.StorageError.NotFound
import domain.UserError.NotFound

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import zio.ZIO
import sttp.tapir.ValidationError
import io.circe.{Encoder, Json}
import io.circe.syntax.*

private val add: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .put
  .in(path[UserId]("memberId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(userNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(addHandler)

private def addHandler(userId: UserId)(storageId: StorageId, memberId: UserId):
  ZIO[StorageMembersRepo & StoragesRepo, DbError.UnexpectedDbError | UserError.NotFound | StorageError.NotFound, Unit] = for
    mStorage <- ZIO.serviceWithZIO[StoragesRepo] {
      _.getById(storageId)
    }
    ownerId <- ZIO.fromOption(mStorage)
      .orElseFail[StorageError.NotFound](StorageError.NotFound(storageId))
      .map(_.ownerId)
    _ <- ZIO.when(ownerId == memberId) {
      ZIO.serviceWithZIO[StorageMembersRepo] {
        _.addMemberToStorageById(storageId, memberId)
      }
    }
  yield ()
