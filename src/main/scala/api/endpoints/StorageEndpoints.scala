package api.endpoints

import api.domain.*
import api.AppEnv

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.tapir.Endpoint
import zio.ZIO

object StorageEndpoints extends IngredientEndpointsErrorOutput:
  case class CreateStorageReqBody(name: String)

  case class StorageSummary(id: StorageId, name: String)

  private val storageNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[StorageError.NotFound]))

  private val myStoragesEndpoint = endpoint
    .in("my" / "storages")
    .securityIn(auth.bearer[UserId]())

  private val getStoragesEndpoint = myStoragesEndpoint
    .get
    .out(jsonBody[List[StorageSummary]])

  private val createStorageEndpoint = myStoragesEndpoint
    .post
    .in(jsonBody[CreateStorageReqBody])
    .out(jsonBody[Storage])

  private val deleteStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[StorageId]("storageId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageSummaryEndpoint = myStoragesEndpoint
    .get
    .in(path[StorageId]("storageId"))
    .out(jsonBody[StorageSummary]())
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageMembersEndpoint = myStoragesEndpoint
    .get
    .in(path[StorageId]("storageId") / "members")
    .out(jsonBody[List[UserId]])
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageIngredientsEndpoint = myStoragesEndpoint
    .get
    .in(path[StorageId]("storageId") / "ingredients")
    .out(jsonBody[List[IngredientId]])
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  private val addIngredientToStorageEndpoint = myStoragesEndpoint
    .put
    .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  private val deleteIngredientFromStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  extension[INPUT, ERROR, OUTPUT, R] (endpoint: Endpoint[UserId, INPUT, ERROR, OUTPUT, R])
    private def zSecuredServerLogic[R0](
      logic: UserId => INPUT => ZIO[R0, ERROR, OUTPUT]
    ) = endpoint.zServerSecurityLogic[R, UserId](ZIO.succeed).serverLogic[R0](logic)

  val endpoints: List[ZServerEndpoint[AppEnv, Any]] = List(
    addIngredientToStorageEndpoint.zSecuredServerLogic(addIngredientToStorage),
    deleteIngredientFromStorageEndpoint.zSecuredServerLogic(deleteMyIngredientFromStorage),
    getStoragesEndpoint.zSecuredServerLogic(getStorages),
    createStorageEndpoint.zSecuredServerLogic(createStorage),
    deleteStorageEndpoint.zSecuredServerLogic(deleteStorage),
    getStorageSummaryEndpoint.zSecuredServerLogic(_ =>
      storageId => ZIO.succeed(StorageSummary(storageId, "Storage"))
    ),
    getStorageMembersEndpoint.zSecuredServerLogic(getStorageMembers),
    getStorageIngredientsEndpoint.zSecuredServerLogic(getStorageIngredients),
    addIngredientToStorageEndpoint.zSecuredServerLogic(addIngredientToStorage),
    deleteIngredientFromStorageEndpoint.zSecuredServerLogic(deleteMyIngredientFromStorage),
  )
