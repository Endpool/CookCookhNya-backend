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
      ingredients.foreach { ingredientId =>
        sql"""
             insert into ${shoppingListTable} (${shoppingListTable.ownerId}, ${shoppingListTable.ingredientId})
             values ($ownerId, $ingredientId)
             on conflict (${shoppingListTable.ownerId}, ${shoppingListTable.ingredientId})
             do nothing
           """.update.run()
        ()
      }
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
      ingredients.foreach { ingredientId =>
        sql"""
             delete from ${shoppingListTable}
             where ${shoppingListTable.ownerId} = $ownerId
             and ${shoppingListTable.ingredientId} = $ingredientId
           """.update.run()
      }
      ()
    }.mapError(handleDbError)

object ShoppingListsRepo:
  val layer = ZLayer.fromFunction(ShoppingListsLive(_))
