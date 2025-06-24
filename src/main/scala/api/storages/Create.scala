package api.storages

import api.AppEnv
import api.zSecuredServerLogic
import db.repositories.StoragesRepo
import domain.{StorageId, UserId}
import domain.DbError.{UnexpectedDbError, DbNotRespondingError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import api.EndpointErrorVariants.{databaseFailureErrorVariant, serverUnexpectedErrorVariant}

final case class CreateStorageReqBody(name: String)

val create: ZServerEndpoint[AppEnv, Any] =
  storagesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[StorageId])
  .errorOut(oneOf(serverUnexpectedErrorVariant, databaseFailureErrorVariant)) // <-- i think it is impossible to
  .zSecuredServerLogic(createHandler)                                         //     get user not found?

private def createHandler(userId: UserId)(reqBody: CreateStorageReqBody):
  ZIO[StoragesRepo, UnexpectedDbError | DbNotRespondingError, StorageId] =
  ZIO.serviceWithZIO[StoragesRepo] {
    _.createEmpty(reqBody.name, userId)
  }
