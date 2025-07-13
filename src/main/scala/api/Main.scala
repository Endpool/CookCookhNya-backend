package api

import _root_.db.dbLayer
import _root_.db.repositories.*
import _root_.db.DataSourceDescription

import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*

import MetricsConfig.metrics
import com.augustnagro.magnum.magzio.Transactor

object Main extends ZIOAppDefault:

  // Configure server options with the metrics interceptor
  private val serverOptions: ZioHttpServerOptions[AppEnv] =
    ZioHttpServerOptions
      .customiseInterceptors[AppEnv]
      .metricsInterceptor(metrics.metricsInterceptor()) // Adds response time tracking
      .options

  // Define Swagger endpoints with the server options
  val swaggerEndpoints: Routes[AppEnv, Response] =
    ZioHttpInterpreter(serverOptions).toHttp(
      SwaggerInterpreter()
        .fromServerEndpoints(
          AppEndpoints.apiEndpoints,
          "CookCookhNya",
          "1.0"
        )
    )

  // Define API endpoints with the server options
  val endpoints: Routes[AppEnv, Response] =
    ZioHttpInterpreter(serverOptions).toHttp(AppEndpoints.apiEndpoints)

  // Combine all routes
  val app: Routes[AppEnv, Response] = endpoints ++ swaggerEndpoints

  // Define the repository layer
  val reposLayer: RLayer[Transactor & InvitationsSecretKey, 
    IngredientsRepo & 
    InvitationsRepo & 
    RecipesDomainRepo & 
    RecipesRepo & 
    RecipeIngredientsRepo & 
    ShoppingListsRepo & 
    StorageIngredientsRepo & 
    StorageMembersRepo & 
    StoragesRepo & 
    UsersRepo] =
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

  // Override the run method to start the server
  override def run: ZIO[Any, Throwable, Unit] =
    Server
      .serve(app)
      .provideSomeLayer[AppEnv](Server.defaultWithPort(8080))
      .provideLayer(
        DataSourceDescription.layerFromEnv >>> dbLayer >+>
        (InvitationsSecretKey.layerFromEnv >>> reposLayer)
      )