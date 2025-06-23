package api.storages

import api.AppEnv
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{Storage, StorageId, UserId, DbError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}
import api.EndpointErrorVariants.serverErrorVariant

final case class CreateStorageReqBody(name: String)

val create: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[StorageId])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(createHandler)

private def createHandler(userId: UserId)(reqBody: CreateStorageReqBody):
  ZIO[StoragesRepo, DbError.UnexpectedDbError, StorageId] =
  ZIO.serviceWithZIO[StoragesRepo] {
    _.createEmpty(reqBody.name, userId)
  }
