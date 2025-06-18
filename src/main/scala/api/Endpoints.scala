package api

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.tapir.ztapir.*
import zio.ZIO

import api.domain.*
import sttp.tapir.Endpoint

object Endpoints:
  val ingredientNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[IngredientError.NotFound]))

  val storageNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[StorageError.NotFound]))

  private val createIngredientsEndpoint = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[Ingredient])
    .out(statusCode(StatusCode.Created))

  private val getIngredientEndpoint = endpoint
    .get
    .in("ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[Ingredient])
    .errorOut(oneOf(ingredientNotFoundVariant))

  private val getAllIngredientsEndpoint = endpoint
    .get
    .in("ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[Ingredient]])

  private val deleteIngredientEndpoint = endpoint
    .delete
    .in("ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(ingredientNotFoundVariant))

  private val myStoragesEndpoint = endpoint
    .in("my" / "storages")
    .securityIn(auth.bearer[UserId]())

  case class StorageSummary(storageId: StorageId, name: String)

  private val getStoragesEndpoint = myStoragesEndpoint
    .get
    .out(jsonBody[List[StorageSummary]])

  case class CreateStorageReqBody(name: String)

  private val createStorageEndpoint = myStoragesEndpoint
    .post
    .in(jsonBody[CreateStorageReqBody])
    .out(jsonBody[Storage])

  private val deleteStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[StorageId]("storageId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageNameEndpoint = myStoragesEndpoint
    .get
    .in(path[StorageId]("storageId") / "name")
    .out(jsonBody[String])
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageMembersEndpoint = myStoragesEndpoint
    .get
    .in(path[StorageId]("storageId") / "members")
    .out(jsonBody[List[UserId]])
    .errorOut(oneOf(storageNotFoundVariant))

  private val getStorageIngredientsEndpoint = myStoragesEndpoint
    .get
    .in(path[Long]("storageId") / "ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[IngredientId]])
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  private val addIngredientToStorageEndpoint = myStoragesEndpoint
    .put
    .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  private val deleteIngredientFromStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[Long]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  extension[INPUT, ERROR, OUTPUT, R]
    (endpoint: Endpoint[UserId, INPUT, ERROR, OUTPUT, R])
    def zSecuredServerLogic[R0](
      logic: UserId => INPUT => ZIO[R0, ERROR, OUTPUT]
    ) = endpoint.zServerSecurityLogic[R, UserId](ZIO.succeed).serverLogic[R0](logic)

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    createIngredientsEndpoint.zServerLogic(createIngredient),
    getIngredientEndpoint.zServerLogic(getIngredient),
    getAllIngredientsEndpoint.zServerLogic(_ => getAllIngredients),
    deleteIngredientEndpoint.zServerLogic(deleteIngredient),

    getStoragesEndpoint.zSecuredServerLogic(getStorages),

    createStorageEndpoint.zSecuredServerLogic(createStorage),
    deleteStorageEndpoint.zSecuredServerLogic(deleteStorage),

    getStorageNameEndpoint.zSecuredServerLogic(getStorageName),
    getStorageMembersEndpoint.zSecuredServerLogic(getStorageMembers),
    getStorageIngredientsEndpoint.zSecuredServerLogic(getStorageIngredients),

    addIngredientToStorageEndpoint.zSecuredServerLogic(addIngredientToStorage),
    deleteIngredientFromStorageEndpoint.zSecuredServerLogic(deleteMyIngredientFromStorage),
  )
