package api

import _root_.db.dbLayer
import _root_.db.repositories.*

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
        IngredientsRepoLive.layer,
        UsersRepo.layer,
        StoragesRepoLive.layer,
        StorageIngredientsRepoLive.layer,
        StorageMembersRepoLive.layer,
        RecipesRepo.layer,
        RecipeIngredientsRepo.layer,
        RecipesDomainRepo.layer,
        ShoppingListsRepo.layer,
        Server.live
      )
