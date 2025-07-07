package api.shoppinglist

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import api.ingredients.IngredientResp
import domain.{IngredientId, InternalServerError, UserId}
import domain.IngredientError.NotFound
import db.repositories.{IngredientsRepo, ShoppingListsRepo}
import db.DbError

import sttp.model.StatusCode
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{Exit, ZIO}

private type GetIngredientsEnv = ShoppingListsRepo & IngredientsRepo

private val getIngredients: ZServerEndpoint[GetIngredientsEnv, Any] = shoppingListEndpoint
  .get
  .out(jsonBody[Seq[IngredientResp]])
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zSecuredServerLogic(getIngredientsHandler)

private def getIngredientsHandler(u: Unit):
  ZIO[GetIngredientsEnv, InternalServerError | NotFound, Seq[IngredientResp]] = {
  for
    userId <- ZIO.succeed(???) // TODO make ShoppingListsRepo require AuthenticatedUser from ZIO environment
    ingredientIds <- ZIO.serviceWithZIO[ShoppingListsRepo](_.getIngredients(userId))
    result <- ZIO.foreach(ingredientIds) {
      ingredientId =>
        ZIO.serviceWithZIO[IngredientsRepo](_.getById(ingredientId)).flatMap {
          case Some(dbEntity) => ZIO.succeed(IngredientResp.fromDb(dbEntity))
          case None => ZIO.fail[NotFound](NotFound(ingredientId.toString))
        }
    }
  yield result
}.mapError {
  case _: DbError  => InternalServerError()
  case e: NotFound => e
}
