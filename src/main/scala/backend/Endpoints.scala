package backend

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*

object Endpoints:
  val ingredientNotFoundVariant = oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[IngredientError.IngredientNotFound]))
  val storageNotFoundVariant = oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[StorageError.StorageNotFound]))

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
    .errorOut {
      oneOf(ingredientNotFoundVariant)
    }

  private val getAllIngredientsEndpoint = endpoint
    .get
    .in("ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[Ingredient]])

  private val deleteIngredientEndpoint = endpoint
    .delete
    .in("ingredients")
    .in(query[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf(ingredientNotFoundVariant)
    }

  private val myStoragesEndpoint = endpoint
    .in("my" / "storages")

  private val addIngredientToStorageEndpoint = myStoragesEndpoint
    .put
    .in(path[StorageId]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf[ErrorResponse](
        ingredientNotFoundVariant,
        storageNotFoundVariant
      )
    }

  private val deleteIngredientFromStorageEndpoint = myStoragesEndpoint
    .delete
    .in(path[Int]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut {
      oneOf(
        ingredientNotFoundVariant,
        storageNotFoundVariant
      )
    }

  private val getAvailableIngredientsFromStorageEndpoint = myStoragesEndpoint
    .get
    .in(path[Int]("storageId") / "ingredients" / path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[List[Ingredient]])
    .errorOut {
      oneOf(
        ingredientNotFoundVariant,
        storageNotFoundVariant
      )
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
