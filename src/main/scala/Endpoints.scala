import sttp.tapir.ztapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import io.circe.generic.auto.*

object Endpoints:
  private val createIngredientsEndpoint = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[Ingredient])
    .out(statusCode(StatusCode.Created))

  private val getIngredientEndpoint = endpoint
    .get
    .in("ingredients")
    .in(query[IngredientId]("id"))
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[Option[Ingredient]])

  private val getAllIngredientsEndpoint = endpoint
    .get
    .in("ingredients")
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[Option[List[Ingredient]]])

  private val deleteIngredientEndpoint = endpoint
    .delete
    .in("ingredients")
    .in(query[IngredientId]("id"))
    .out(statusCode(StatusCode.NoContent))
    .out(stringBody)

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    createIngredientsEndpoint.zServerLogic(createIngredient)       ::
    getIngredientEndpoint.zServerLogic(getIngredient)              ::
    getAllIngredientsEndpoint.zServerLogic(_ => getAllIngredients) ::
    deleteIngredientEndpoint.zServerLogic(deleteIngredient)        :: Nil
