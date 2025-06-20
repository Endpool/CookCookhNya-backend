package api.endpoints.storage

import api.endpoints.SecureEndpointLogicProvider.zSecuredServerLogic
import api.db.repositories.StorageRepoInterface
import api.domain.{StorageView, UserId}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

val getStoragesEndpoint: ZServerEndpoint[StorageRepoInterface, Any] = myStoragesEndpoint
  .get
  .out(jsonBody[Seq[StorageView]])
  .zSecuredServerLogic(getStorages)

private def getStorages(userId: UserId): Unit => URIO[StorageRepoInterface, Seq[StorageView]] =
  _ => ZIO.serviceWithZIO[StorageRepoInterface](_.getAllStorageViews)
