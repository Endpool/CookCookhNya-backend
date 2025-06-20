package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageRepoInterface
import api.domain.{Storage, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

case class CreateStorageReqBody(name: String)

val createStorageEndpoint: ZServerEndpoint[StorageRepoInterface, Any] = myStoragesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[Storage])
  .zSecuredServerLogic(createStorage)

private def createStorage(userId: UserId)(reqBody: CreateStorageReqBody):
URIO[StorageRepoInterface, Storage] =
  ZIO.serviceWithZIO[StorageRepoInterface] {
    _.createEmpty(Storage.CreationEntity(reqBody.name, userId))
  }
