package api

import _root_.db.dbLayer
import _root_.db.repositories.*
import _root_.db.DataSourceDescription

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

  val app: Routes[AppEnv, Response] = endpoints ++ swaggerEndpoints

  override def run =
    Server.serve(app)
      .provide(
        Server.defaultWithPort(8080),
        DataSourceDescription.layerFromEnv >>> dbLayer,
        IngredientsRepo.layer,
        UsersRepo.layer,
        StoragesRepo.layer,
        StorageIngredientsRepo.layer,
        StorageMembersRepo.layer,
        RecipesRepo.layer,
        RecipeIngredientsRepo.layer
      )
