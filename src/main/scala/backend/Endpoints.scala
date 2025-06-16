package backend

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*

import scala.reflect.ClassTag

object Endpoints:
  private def matchErrorWithStatusCode[T <: ErrorResponse: ClassTag](statusCode: StatusCode,
                                                                     output: EndpointOutput[ErrorResponse]) =
    oneOfVariantValueMatcher(statusCode, output) {
      implicitly[ClassTag[T]].runtimeClass.isInstance(_)
    }

  private val createIngredientsEndpoint = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[Ingredient])
    .out(statusCode(StatusCode.Created))

  private val getIngredientEndpoint = endpoint
    .get
    .in("ingredients" / path[IngredientId]("id"))
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[Ingredient])
    .errorOut {
      oneOf {
        matchErrorWithStatusCode[IngredientNotFound](StatusCode.NotFound, jsonBody[ErrorResponse])
      }
    }

  private val getAllIngredientsEndpoint = endpoint
    .get
    .in("ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[Ingredient]])

  private val deleteIngredientEndpoint = endpoint
    .delete
    .in("ingredients")
    .in(query[IngredientId]("id"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf {
        matchErrorWithStatusCode[IngredientNotFound](StatusCode.NotFound, jsonBody[ErrorResponse])
      }
    }

  private val myStoragesEndpoint = endpoint
    .in("my" / "storages")

  private val addIngredientToStorageEndpoint = myStoragesEndpoint
    .put
    .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf(
        matchErrorWithStatusCode[IngredientNotFound](StatusCode.NotFound, jsonBody[ErrorResponse]),
        matchErrorWithStatusCode[StorageNotFound](StatusCode.NotFound, jsonBody[ErrorResponse])
      )
    }

  private val deleteIngredientFromStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[Int]("storageId") / "ingredients" / path[IngredientId]("id"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf(
        matchErrorWithStatusCode[IngredientNotFound](StatusCode.NotFound, jsonBody[ErrorResponse]),
        matchErrorWithStatusCode[StorageNotFound](StatusCode.NotFound, jsonBody[ErrorResponse])
      )
    }

  private val getAvailableIngredientsFromStorageEndpoint = myStoragesEndpoint
    .get
    .in(path[Int]("storageId") / "ingredients" / path[IngredientId]("id"))
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[Ingredient]])
    .errorOut {
      oneOf {
        matchErrorWithStatusCode[StorageNotFound](StatusCode.NotFound, jsonBody[ErrorResponse])
      }
    }

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    createIngredientsEndpoint.zServerLogic(createIngredient),
    getIngredientEndpoint.zServerLogic(getIngredient),
    getAllIngredientsEndpoint.zServerLogic(_ => getAllIngredients),
    deleteIngredientEndpoint.zServerLogic(deleteIngredient),
    addIngredientToStorageEndpoint.zServerLogic(addIngredientToStorage),
    deleteIngredientFromStorageEndpoint.zServerLogic(deleteMyIngredientFromStorage),
    getAvailableIngredientsFromStorageEndpoint.zServerLogic(getAvailableIngredientsFromStorage)
  )
