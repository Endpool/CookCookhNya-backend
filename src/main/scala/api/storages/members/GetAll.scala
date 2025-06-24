package api.storages.members

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import api.zSecuredServerLogic
import db.repositories.StorageMembersRepo
import domain.{StorageError, StorageId, UserId, DbError}

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .get
  .out(jsonBody[Seq[UserId]])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageMembersRepo, DbError.UnexpectedDbError, Seq[UserId]] =
  ZIO.serviceWithZIO[StorageMembersRepo] {
    _.getAllStorageMembers(storageId)
  }
