package api.users

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

import api.zSecuredServerLogic
import api.EndpointErrorVariants.serverErrorVariant
import api.AppEnv
import db.repositories.UsersRepo
import domain.{DbError, UserId}

final case class CreateUserReqBody(alias: Option[String], fullName: String)

val create: ZServerEndpoint[AppEnv, Any] =
  usersEndpoint
    .put
    .in(jsonBody[CreateUserReqBody])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(userId: UserId)(reqBody: CreateUserReqBody): ZIO[UsersRepo, DbError.UnexpectedDbError, Unit] =
  ZIO.serviceWithZIO[UsersRepo](_.saveUser(userId, reqBody.alias, reqBody.fullName))