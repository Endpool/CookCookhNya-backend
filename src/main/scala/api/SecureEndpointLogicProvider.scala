package api

import domain.UserId

import sttp.tapir.ztapir.RichZEndpoint
import sttp.tapir.Endpoint
import zio.ZIO

extension [INPUT, ERROR, OUTPUT, R](endpoint: Endpoint[UserId, INPUT, ERROR, OUTPUT, R])
  def zSecuredServerLogic[R0](logic: UserId => INPUT => ZIO[R0, ERROR, OUTPUT]) =
    endpoint.zServerSecurityLogic[R, UserId](ZIO.succeed).serverLogic[R0](logic)
