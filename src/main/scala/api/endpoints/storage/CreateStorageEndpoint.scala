package api.endpoints.storage

import api.AppEnv
import api.db.repositories.IStoragesRepo
import api.domain.{Storage, UserId}
import api.endpoints.zSecuredServerLogic

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

final case class CreateStorageReqBody(name: String)

val createStorageEndpoint: ZServerEndpoint[AppEnv, Any] = myStoragesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[Storage])
  .zSecuredServerLogic(createStorage)

private def createStorage(userId: UserId)(reqBody: CreateStorageReqBody):
  URIO[IStoragesRepo, Storage] =
  ZIO.serviceWithZIO[IStoragesRepo] {
    _.createEmpty(reqBody.name, userId)
  }
