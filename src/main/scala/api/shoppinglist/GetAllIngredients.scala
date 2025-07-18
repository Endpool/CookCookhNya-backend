package api.shoppinglist

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import api.common.search.*
import api.ingredients.IngredientResp
import db.repositories.{IngredientsRepo, ShoppingListsRepo}
import db.DbError
import domain.{IngredientNotFound, IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type GetIngredientsEnv = ShoppingListsRepo & IngredientsRepo

private val getIngredients: ZServerEndpoint[GetIngredientsEnv, Any] =
  shoppingListEndpoint
  .get
  .in(PaginationParams.query)
  .out(jsonBody[Seq[IngredientResp]])
  .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
  .zSecuredServerLogic(getIngredientsHandler)

private def getIngredientsHandler(paginationParams: PaginationParams):
  ZIO[AuthenticatedUser & GetIngredientsEnv,
      InternalServerError | IngredientNotFound,
      Seq[IngredientResp]] = {
  for
    ingredientIds <- ZIO.serviceWithZIO[ShoppingListsRepo](_.getIngredients)
    result <- ZIO.foreachPar(ingredientIds) { ingredientId =>
        ZIO.serviceWithZIO[IngredientsRepo](_
          .get(ingredientId)
          .someOrFail(IngredientNotFound(ingredientId.toString))
          .map(IngredientResp.fromDb)
        )
    }.map(_.sortBy(_.name))
  yield result.paginate(paginationParams)
}.mapError {
  case _: DbError  => InternalServerError()
  case e: IngredientNotFound => e
}
