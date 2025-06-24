package api.storages

import api.AppEnv
import api.EndpointErrorVariants.{
  databaseFailureErrorVariant,
  serverUnexpectedErrorVariant,
  storageNotFoundVariant
}
import api.zSecuredServerLogic
import db.repositories.StorageMembersRepo
import domain.StorageError.NotFound
import domain.DbError.{UnexpectedDbError, DbNotRespondingError}
import domain.{StorageId, UserId}

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getMembers: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .in(path[StorageId]("storageId") / "members")
  .out(jsonBody[Seq[UserId]])
  .errorOut(oneOf(serverUnexpectedErrorVariant, databaseFailureErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getMembersHandler)

private def getMembersHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageMembersRepo, UnexpectedDbError | DbNotRespondingError | NotFound, Seq[UserId]] =
  ZIO.serviceWithZIO[StorageMembersRepo] {
    _.getAllStorageMembers(storageId)
  }
