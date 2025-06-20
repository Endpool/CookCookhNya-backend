package api.users

import sttp.tapir.ztapir.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import zio.{URIO, ZIO}

import api.GeneralEndpointData.serverErrorVariant
import api.AppEnv
import db.repositories.UsersRepo
import domain.{DbError, UserId}

final case class CreateUserReqBody(username: String)

val create: ZServerEndpoint[AppEnv, Any] =
  usersEndpoint
    .post
    .in(jsonBody[CreateUserReqBody])
    .out(plainBody[UserId])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(createHandler)
  
private def createHandler(reqBody: CreateUserReqBody): ZIO[UsersRepo, DbError.UnexpectedDbError, UserId] =
  ZIO.serviceWithZIO[UsersRepo](_.addUserIfNotExists(reqBody.username))