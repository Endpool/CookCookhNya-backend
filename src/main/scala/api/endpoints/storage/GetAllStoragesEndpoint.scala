package api.endpoints.storage

import api.endpoints.zSecuredServerLogic
import api.db.repositories.IStoragesRepo
import api.domain.{StorageView, UserId}
import api.AppEnv

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

val getStoragesEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .get
  .out(jsonBody[Seq[StorageView]])
  .zSecuredServerLogic(getStorages)

private def getStorages(userId: UserId)(u : Unit) : URIO[IStoragesRepo, Seq[StorageView]] =
  ZIO.serviceWithZIO[IStoragesRepo](_.getAllStorageViews)
