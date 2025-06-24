package api.storages.members

import api.AppEnv
import api.EndpointErrorVariants.{userNotFoundVariant, storageNotFoundVariant, serverErrorVariant}
import api.zSecuredServerLogic
import db.repositories.StorageMembersRepo
import domain.{UserError, StorageError, StorageId, UserId, DbError}
import domain.StorageError.NotFound
import domain.UserError.NotFound

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import zio.ZIO

private val add: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .put
  .in(path[UserId]("memberId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(userNotFoundVariant, storageNotFoundVariant, serverErrorVariant))
  .zSecuredServerLogic(addHandler)

private def addHandler(userId: UserId)(storageId: StorageId, memberId: UserId):
  ZIO[StorageMembersRepo, UserError.NotFound | StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[StorageMembersRepo] {
    _.addMemberToStorageById(storageId, memberId)
  }
