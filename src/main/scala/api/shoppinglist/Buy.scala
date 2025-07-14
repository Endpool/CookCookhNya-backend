package api.shoppinglist

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant, storageNotFoundVariant}
import api.{handleFailedSqlQuery, toIngredientNotFound, toStorageNotFound}
import common.OptionExtensions.<|>
import db.DbError.*
import db.repositories.{ShoppingListsRepo, StorageMembersRepo}
import domain.{IngredientId, IngredientNotFound, InternalServerError, StorageId, StorageNotFound}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type BuyEnv = ShoppingListsRepo & StorageMembersRepo

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
  ZIO[AuthenticatedUser & BuyEnv,
      InternalServerError | IngredientNotFound | StorageNotFound,
      Unit] = for
  userIsMemberOfStorage <- ZIO.serviceWithZIO[StorageMembersRepo](_
    .checkForMembership(storageId)
    .orElseFail(InternalServerError())
  )
  _ <- ZIO.fail(StorageNotFound(storageId.toString))
    .unless(userIsMemberOfStorage)

  _ <- ZIO.serviceWithZIO[ShoppingListsRepo](_
    .buyIngredientsToStorage(ingredientIds, storageId)
    .mapError {
      case _: DbNotRespondingError => InternalServerError()
      case e: FailedDbQuery => handleFailedSqlQuery(e)
        .flatMap(fkv => toIngredientNotFound(fkv) <|> toStorageNotFound(fkv))
        .getOrElse(InternalServerError())
    }
  )
yield ()
