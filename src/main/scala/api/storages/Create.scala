package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import db.repositories.StoragesRepo
import domain.{InternalServerError, StorageId, UserId}
import db.DbError
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import api.EndpointErrorVariants.serverErrorVariant

final case class CreateStorageReqBody(name: String)

private type CreateEnv = StoragesRepo

private val create: ZServerEndpoint[CreateEnv, Any] =
  storagesEndpoint
  .post
  .in(jsonBody[CreateStorageReqBody])
  .out(jsonBody[StorageId])
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateStorageReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError, StorageId] =
  val userId = ???
  ZIO.serviceWithZIO[StoragesRepo] {
    _.createEmpty(reqBody.name, userId)
  }.mapError(_ => InternalServerError())
