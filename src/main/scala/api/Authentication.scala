package api

import domain.UserId

import sttp.tapir.{Codec, Endpoint}
import sttp.tapir.ztapir.{RichZEndpoint, auth}
import zio.ZIO
import zio.ZLayer

object Authentication:
  case class AuthenticatedUser private(userId: UserId)
  object AuthenticatedUser:
     def createFromUserId(userId: UserId): AuthenticatedUser = AuthenticatedUser(userId)

     given Codec.PlainCodec[AuthenticatedUser] = Codec.long.map(createFromUserId)(_.userId)

  extension [INPUT, ERROR, OUTPUT, R](endpoint: Endpoint[Unit, INPUT, ERROR, OUTPUT, R])
    def zSecuredServerLogic[R0](logic: INPUT => ZIO[AuthenticatedUser & R0, ERROR, OUTPUT]) =
      endpoint
        .securityIn(auth.bearer[AuthenticatedUser]())
        .zServerSecurityLogic[R, AuthenticatedUser](ZIO.succeed)
        .serverLogic[R0](authenticatedUser => input =>
          logic(input).provideSomeLayer[R0](ZLayer.succeed(authenticatedUser))
        )
