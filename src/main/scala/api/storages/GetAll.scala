package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{StorageView, UserId}
import api.zSecuredServerLogic

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .out(jsonBody[Seq[StorageView]])
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(u : Unit) : URIO[StoragesRepo, Seq[StorageView]] =
  ZIO.serviceWithZIO[StoragesRepo](_.getAllStorageViews)
