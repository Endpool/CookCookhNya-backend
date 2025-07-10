package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.StoragesRepo
import db.DbError
import domain.{InternalServerError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

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
  ZIO.serviceWithZIO[StoragesRepo](_
    .createEmpty(reqBody.name)
    .orElseFail(InternalServerError())
  )
