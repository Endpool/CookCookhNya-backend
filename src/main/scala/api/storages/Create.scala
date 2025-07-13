package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{userNotFoundVariant, serverErrorVariant}
import api.{handleFailedSqlQuery, toUserNotFound}
import db.DbError
import db.repositories.StoragesRepo
import domain.{UserNotFound, InternalServerError, StorageId}

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
  .out(plainBody[StorageId])
  .errorOut(oneOf(userNotFoundVariant, serverErrorVariant))
  .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateStorageReqBody):
  ZIO[AuthenticatedUser & CreateEnv, UserNotFound | InternalServerError, StorageId] =
  ZIO.serviceWithZIO[StoragesRepo](_
    .createEmpty(reqBody.name)
    .mapError {
      case e: DbError.FailedDbQuery => handleFailedSqlQuery(e)
        .flatMap(toUserNotFound)
        .getOrElse(InternalServerError())
      case _: DbError.DbNotRespondingError => InternalServerError()
    }
  )
