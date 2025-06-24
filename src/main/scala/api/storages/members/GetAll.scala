package api.storages.members

import api.AppEnv
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.repositories.StorageMembersRepo
import domain.{StorageError, StorageId, UserId, DbError}

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import db.repositories.StoragesRepo

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .get
  .out(jsonBody[Seq[UserId]])
  .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageMembersRepo & StoragesRepo, DbError.UnexpectedDbError | StorageError.NotFound, Seq[UserId]] = for
    memberIds <- ZIO.serviceWithZIO[StorageMembersRepo] {
      _.getAllStorageMembers(storageId)
    }
    mStorage <- ZIO.serviceWithZIO[StoragesRepo] {
      _.getById(storageId)
    }
    ownerId <- ZIO.fromOption(mStorage)
      .orElseFail[StorageError.NotFound](StorageError.NotFound(storageId))
      .map(_.ownerId)
  yield ownerId +: memberIds
