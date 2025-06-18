package api.endpoints

import api.domain.*
import api.AppEnv

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

object StoragesIngredientEndpoints extends IngredientEndpointsErrorOutput:
  private val storageNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[StorageError.NotFound]))

  private val myStoragesEndpoint = endpoint
    .in("my" / "storages")

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

  private val getAllIngredientsFromStorageEndpoint = myStoragesEndpoint
    .get
    .in(path[Long]("storageId") / "ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[Seq[IngredientId]])
    .errorOut(oneOf(ingredientNotFoundVariant, storageNotFoundVariant))

  val endpoints: List[ZServerEndpoint[AppEnv, Any]] = List(
    addIngredientToStorageEndpoint.zServerLogic(addIngredientToStorage),
    deleteIngredientFromStorageEndpoint.zServerLogic(deleteMyIngredientFromStorage),
    getAllIngredientsFromStorageEndpoint.zServerLogic(getAllIngredientsFromStorage)
  )
