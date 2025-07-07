package api.users

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.serverErrorVariant
import db.DbError
import db.repositories.UsersRepo
import domain.{InternalServerError, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class CreateUserReqBody(alias: Option[String], fullName: String)

private type CreateEnv = UsersRepo

private val create: ZServerEndpoint[CreateEnv, Any] =
  usersEndpoint
    .put
    .in(jsonBody[CreateUserReqBody])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateUserReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError, Unit] =
  ZIO.serviceWithZIO[UsersRepo](_.saveUser(reqBody.alias, reqBody.fullName))
    .mapError(e => InternalServerError(e.message))
