package api.shoppinglist

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import api.ingredients.IngredientResp
import domain.{IngredientNotFound, IngredientId, InternalServerError}
import db.repositories.{IngredientsRepo, ShoppingListsRepo}
import db.DbError

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetIngredientsEnv = ShoppingListsRepo & IngredientsRepo

private val getIngredients: ZServerEndpoint[GetIngredientsEnv, Any] =
  shoppingListEndpoint
  .get
  .out(jsonBody[Seq[IngredientResp]])
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zSecuredServerLogic(getIngredientsHandler)

private def getIngredientsHandler(u: Unit):
  ZIO[AuthenticatedUser & GetIngredientsEnv,
      InternalServerError | IngredientNotFound,
      Seq[IngredientResp]] = {
  for
    ingredientIds <- ZIO.serviceWithZIO[ShoppingListsRepo](_.getIngredients)
    result <- ZIO.foreach(ingredientIds) { ingredientId =>
        ZIO.serviceWithZIO[IngredientsRepo](_
          .get(ingredientId)
          .someOrFail(IngredientNotFound(ingredientId.toString))
          .map(IngredientResp.fromDb)
        )
    }
  yield result
}.mapError {
  case _: DbError  => InternalServerError()
  case e: IngredientNotFound => e
}
