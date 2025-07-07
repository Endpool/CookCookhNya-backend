package api

import domain.UserId

import sttp.tapir.ztapir.RichZEndpoint
import sttp.tapir.Endpoint
import zio.ZIO
import zio.ZLayer

object Authentication:
  opaque type AuthenticatedUser = Long
  object AuthenticatedUser:
    extension (authenticatedUser: AuthenticatedUser)
      def userId: UserId = authenticatedUser

  extension [INPUT, ERROR, OUTPUT, R](endpoint: Endpoint[UserId, INPUT, ERROR, OUTPUT, R])
    def zSecuredServerLogic[R0](logic: INPUT => ZIO[AuthenticatedUser & R0, ERROR, OUTPUT]) =
      endpoint
        .zServerSecurityLogic[R, AuthenticatedUser](ZIO.succeed)
        .serverLogic[R0](authenticatedUser => input =>
          logic(input).provideSomeLayer(ZLayer.succeed(authenticatedUser))
        )
