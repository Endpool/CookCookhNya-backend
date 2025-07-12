package api

import _root_.db.{dbLayer, dataSourceLayer, DataSourceDescription}
import _root_.db.repositories.*

import com.augustnagro.magnum.magzio.Transactor
import javax.sql.DataSource
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

  val reposLayer:
    RLayer[Transactor & DataSource & InvitationsSecretKey
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
    RecipeIngredientsRepoLive.layer ++
    RecipesDomainRepo.layer ++
    RecipesRepo.layer ++
    ShoppingListsRepo.layer ++
    StorageIngredientsRepo.layer ++
    StorageMembersRepo.layer ++
    StoragesRepo.layer ++
    UsersRepo.layer

  override def run: IO[Throwable, Nothing] =
    Server.serve(app)
      .provideSomeLayer(Server.defaultWithPort(8080))
      .provideLayer(
        DataSourceDescription.layerFromEnv >>> dataSourceLayer >+> dbLayer >+>
        (InvitationsSecretKey.layerFromEnv >>> reposLayer)
      )
