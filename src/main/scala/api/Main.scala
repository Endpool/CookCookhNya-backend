package api

import _root_.db.dbLayer
import _root_.db.repositories.*
import _root_.db.DataSourceDescription

import sttp.tapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*
import com.augustnagro.magnum.magzio.Transactor
import sttp.tapir.server.ziohttp.ZioHttpServerOptions

object Main extends ZIOAppDefault:
  val serverOptions: ZioHttpServerOptions[AppEnv] =
    ZioHttpServerOptions
      .customiseInterceptors[AppEnv]
      .metricsInterceptor(MetricsConfig.metrics.metricsInterceptor())
      .options

  val swaggerEndpoints: Routes[AppEnv, Response] = ZioHttpInterpreter(serverOptions).toHttp(
    SwaggerInterpreter()
      .fromServerEndpoints(
        AppEndpoints.endpoints,
        "CookCookhNya",
        "1.0"
      )
  )

  val endpoints: Routes[AppEnv, Response] =
    ZioHttpInterpreter(serverOptions).toHttp(AppEndpoints.endpoints)

  val metricsEndpoints: Routes[AppEnv, Response] =
    ZioHttpInterpreter(serverOptions).toHttp(List(MetricsConfig.metrics.metricsEndpoint))

  val app: Routes[AppEnv, Response] =
    swaggerEndpoints ++ endpoints ++ metricsEndpoints

  val reposLayer:
    RLayer[Transactor & InvitationsSecretKey
      , IngredientsRepo
      & InvitationsRepo
      & RecipesDomainRepo
      & RecipesRepo
      & RecipeIngredientsRepo
      & ShoppingListsRepo
      & StorageIngredientsRepo
      & StorageMembersRepo
      & StoragesRepo
      & UsersRepo
    ] =
    IngredientsRepo.layer ++
    InvitationsRepo.layer ++
    RecipeIngredientsRepo.layer ++
    RecipesDomainRepo.layer ++
    RecipesRepo.layer ++
    ShoppingListsRepo.layer ++
    StorageIngredientsRepo.layer ++
    StorageMembersRepo.layer ++
    StoragesRepo.layer ++
    UsersRepo.layer

  override def run =
    Server.serve(app)
      .provideSomeLayer(Server.defaultWithPort(8080))
      .provideLayer(
        DataSourceDescription.layerFromEnv >>> dbLayer >+>
        (InvitationsSecretKey.layerFromEnv >>> reposLayer)
      )
