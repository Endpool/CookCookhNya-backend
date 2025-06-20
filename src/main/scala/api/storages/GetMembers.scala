package api.storages

import api.AppEnv
import api.EndpointErrorVariants.storageNotFoundVariant
import api.zSecuredServerLogic
import db.repositories.StorageMembersRepo
import domain.{StorageError, StorageId, UserId}

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private val getMembers: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .in(path[StorageId]("storageId") / "members")
  .out(jsonBody[Seq[UserId]])
  .errorOut(oneOf(storageNotFoundVariant))
  .zSecuredServerLogic(getMembersHandler)

private def getMembersHandler(userId: UserId)(storageId: StorageId):
  ZIO[StorageMembersRepo, StorageError.NotFound, Seq[UserId]] =
  ZIO.serviceWithZIO[StorageMembersRepo] {
    _.getAllStorageMembers(storageId)
  }
