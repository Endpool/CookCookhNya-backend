package db.repositories

import db.tables.{DbShoppingList, shoppingListTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, Recipe, RecipeId, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{ZIO, ZLayer}

trait ShoppingListsRepo:
  def addIngredients(ownerId: UserId, ingredients: Seq[IngredientId]):
  ZIO[ShoppingListsRepo, DbError, Unit]

  def getIngredients(ownerId: UserId): ZIO[ShoppingListsRepo, DbError, Vector[IngredientId]]

  def deleteIngredients(ownerId: UserId, ingredients: Seq[IngredientId]):
  ZIO[ShoppingListsRepo, DbError, Unit]

private final case class ShoppingListsLive(xa: Transactor)
  extends Repo[DbShoppingList, DbShoppingList, Null] with ShoppingListsRepo:

  override def addIngredients(ownerId: UserId, ingredients: Seq[IngredientId]):
  ZIO[ShoppingListsRepo, DbError, Unit] =
    xa.transact {
      insertAll(ingredients.map(ingredientId => DbShoppingList(ownerId, ingredientId)))
    }.mapError(handleDbError)

  override def getIngredients(ownerId: UserId):
  ZIO[ShoppingListsRepo, DbError, Vector[IngredientId]] =
    xa.transact {
      sql"""
           select ${shoppingListTable.ingredientId} from $shoppingListTable
           where ${shoppingListTable.ownerId} = $ownerId
         """.query[IngredientId].run()
    }.mapError(handleDbError)

  override def deleteIngredients(ownerId: UserId, ingredients: Seq[IngredientId]):
  ZIO[ShoppingListsRepo, DbError, Unit] =
    xa.transact {
      deleteAll(ingredients.map(ingredientId => DbShoppingList(ownerId, ingredientId)))
      ()
    }.mapError(handleDbError)

object ShoppingListsRepo:
  val layer = ZLayer.fromFunction(ShoppingListsLive(_))
