package api

import _root_.db.{dbLayer, dataSourceLayer, DataSourceDescription}
import _root_.db.repositories.*

import com.augustnagro.magnum.magzio.Transactor
import javax.sql.DataSource
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*
import zio.logging.{consoleLogger, LogFormat}
import zio.logging.ConsoleLoggerConfig
import sttp.tapir.server.interceptor.log.DefaultServerLog

object Main extends ZIOAppDefault:
  type AppEnvRIO[A] = RIO[AppEnv, A]
  val serverLog = DefaultServerLog[AppEnvRIO](
    doLogWhenReceived = ZIO.logInfo,
    doLogWhenHandled = (msg, error) =>
      if error.isDefined
        then ZIO.logError(msg)
        else ZIO.logInfo(msg),
    doLogAllDecodeFailures = (msg, _) => ZIO.logDebug(msg),
    doLogExceptions = (msg, _) => ZIO.logError(msg),
    noLog = ZIO.unit,
  )

  val serverOptions: ZioHttpServerOptions[AppEnv] =
    ZioHttpServerOptions
      .customiseInterceptors[AppEnv]
      .metricsInterceptor(MetricsConfig.metrics.metricsInterceptor())
      .serverLog(serverLog)
      .options

  val httpInterpreter: ZioHttpInterpreter[AppEnv] =
    ZioHttpInterpreter(serverOptions)

  val swaggerEndpoints: Routes[AppEnv, Response] = httpInterpreter.toHttp(
    SwaggerInterpreter()
      .fromServerEndpoints(
        AppEndpoints.endpoints,
        "CookCookhNya",
        "1.0"
      )
  )

  val endpoints: Routes[AppEnv, Response] =
    httpInterpreter.toHttp(AppEndpoints.endpoints)

  val metricsEndpoints: Routes[AppEnv, Response] =
    httpInterpreter.toHttp(List(MetricsConfig.metrics.metricsEndpoint))

  val app: Routes[AppEnv, Response] =
    swaggerEndpoints ++ endpoints ++ metricsEndpoints

  val reposLayer:
    RLayer[Transactor & DataSource & InvitationsSecretKey
      , IngredientsRepo
      & InvitationsRepo
      & RecipesDomainRepo
      & RecipesRepo
      & RecipeIngredientsRepo
      & RecipePublicationRequestsRepo
      & ShoppingListsRepo
      & StorageIngredientsRepo
      & StorageMembersRepo
      & StoragesRepo
      & UsersRepo
    ] =
    IngredientsRepo.layer ++
    InvitationsRepo.layer ++
    RecipeIngredientsRepoLive.layer ++
    RecipesDomainRepo.layer ++
    RecipesRepo.layer ++
    RecipePublicationRequestsRepo.layer ++
    ShoppingListsRepo.layer ++
    StorageIngredientsRepo.layer ++
    StorageMembersRepo.layer ++
    StoragesRepo.layer ++
    UsersRepo.layer

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig.default.copy(format=LogFormat.colored)
    )

  val port: Int = 8080

  override def run: IO[Throwable, Nothing]
    =  ZIO.logInfo(s"Starting server on port $port")
    *> Server.serve(app)
      .provideSomeLayer(Server.defaultWithPort(port))
      .provideLayer(
        DataSourceDescription.layerFromEnv >>> dataSourceLayer >+> dbLayer >+>
        (InvitationsSecretKey.layerFromEnv >>> reposLayer)
      )
      .tapErrorCause(ZIO.logErrorCause("Server failed", _))
