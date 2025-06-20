package api

import api.endpoints.AppEndpoints
import api.db.dbLayer
import api.AppEnv
import api.db.repositories.*

import sttp.tapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*

object Main extends ZIOAppDefault:
  val swaggerEndpoints: Routes[AppEnv, Response] = ZioHttpInterpreter().toHttp(
    SwaggerInterpreter()
      .fromServerEndpoints(
        AppEndpoints.endpoints,
        "CookCookhNya",
        "1.0"
      )
  )

  val endpoints: Routes[AppEnv, Response] =
    ZioHttpInterpreter().toHttp(AppEndpoints.endpoints)

  val app = endpoints ++ swaggerEndpoints
  override def run =
    Server.serve(app)
      .provide(
        ZLayer.succeed(Server.Config.default.port(8080)),
        dbLayer,
        IngredientsRepo.layer,
        StoragesRepo.layer,
        StorageIngredientsRepo.layer,
        StorageMembersRepo.layer,
        Server.live
      )
      .exitCode
