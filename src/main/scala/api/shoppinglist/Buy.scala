package api.shoppinglist

import api.{handleFailedSqlQuery, toIngredientNotFound, toStorageNotFound}
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant, storageNotFoundVariant}
import domain.{IngredientId, IngredientNotFound, InternalServerError, StorageId, StorageNotFound}
import db.repositories.ShoppingListsRepo
import db.DbError.*
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type BuyEnv = ShoppingListsRepo
private val buy: ZServerEndpoint[BuyEnv, Any] =
  shoppingListEndpoint
    .put
    .in("buy")
    .in(query[StorageId]("storage-id"))
    .in(query[List[IngredientId]]("ingredient-id"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant, storageNotFoundVariant))
    .zSecuredServerLogic(buyHandler)

def buyHandler(storageId: StorageId, ingredientIds: List[IngredientId]):
  ZIO[AuthenticatedUser & BuyEnv, InternalServerError | IngredientNotFound | StorageNotFound, Unit] = ZIO.serviceWithZIO[ShoppingListsRepo](
    _.buyIngredientsToStorage(ingredientIds, storageId)
  ).mapError {
    case _: DbNotRespondingError => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toIngredientNotFound)
      .orElse(
        handleFailedSqlQuery(e).flatMap(toStorageNotFound)
      )
      .getOrElse(InternalServerError())
  }