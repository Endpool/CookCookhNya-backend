import sttp.tapir.ztapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import io.circe.generic.auto.*

object Endpoints:
  private val createIngredientsEndpoint = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[Ingredient])
    .out(stringBody)

  private val getIngredientEndpoint = endpoint
    .get
    .in("ingredients")
    .in(query[IngredientId]("id"))
    .out(jsonBody[Option[Ingredient]])

  private val getAllIngredientsEndpoint = endpoint
    .get
    .in("ingredients")
    .out(jsonBody[Option[List[Ingredient]]])

  private val deleteIngredientEndpoint = endpoint
    .delete
    .in("ingredients")
    .in(query[IngredientId]("id"))
    .out(stringBody)

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    createIngredientsEndpoint.zServerLogic(createIngredient) ::
    getIngredientEndpoint.zServerLogic(getIngredient) ::
    getAllIngredientsEndpoint.zServerLogic(_ => getAllIngredients) ::
    deleteIngredientEndpoint.zServerLogic(deleteIngredient) :: Nil
