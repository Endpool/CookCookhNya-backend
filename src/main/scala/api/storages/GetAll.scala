package api.storages

import api.AppEnv
import db.repositories.StoragesRepo
import domain.{UserId, StorageId}
import api.zSecuredServerLogic

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .get
  .out(jsonBody[Seq[StorageSummaryResp]])
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(u : Unit) : URIO[StoragesRepo, Seq[StorageSummaryResp]] =
  ZIO.serviceWithZIO[StoragesRepo](_.getAll(userId).map(_.map(dbToResp)))

