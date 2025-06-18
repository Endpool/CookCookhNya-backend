package api

import sttp.tapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*

object Main extends ZIOAppDefault:
  val swaggerEndpoints: Routes[Any, Response] = ZioHttpInterpreter().toHttp(
    SwaggerInterpreter()
      .fromServerEndpoints(
        Endpoints.endpoints,
        "CookCookhNya",
        "1.0"
      )
  )

  val endpoints: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(Endpoints.endpoints)

  val app = endpoints ++ swaggerEndpoints
  override def run: URIO[Any, ExitCode] =
    Server.serve(app)
      .provide(
        ZLayer.succeed(Server.Config.default.port(8080)),
        Server.live
      )
      .exitCode
