package api.storages

import api.AppEnv
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{Storage, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

final case class CreateStorageReqBody(name: String)

val create: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[Storage])
  .zSecuredServerLogic(createHandler)

private def createHandler(userId: UserId)(reqBody: CreateStorageReqBody):
  URIO[StoragesRepo, Storage] =
  ZIO.serviceWithZIO[StoragesRepo] {
    _.createEmpty(reqBody.name, userId)
  }
