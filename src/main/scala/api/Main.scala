package api

import _root_.db.dbLayer
import _root_.db.repositories.*
import _root_.db.DataSourceDescription

import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.metrics.prometheus.PrometheusMetrics
import zio.*
import zio.http.*
import com.augustnagro.magnum.magzio.Transactor

object Main extends ZIOAppDefault:
  private val metrics = PrometheusMetrics.default[Task]()

  private val serverOptions: ZioHttpServerOptions[AppEnv] =
    ZioHttpServerOptions
      .customiseInterceptors[AppEnv]
      .metricsInterceptor(metrics.metricsInterceptor()) 
      .options

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
