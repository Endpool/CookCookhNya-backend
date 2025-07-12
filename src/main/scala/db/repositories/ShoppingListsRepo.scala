package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbShoppingList, shoppingListTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{ZIO, ZLayer}

trait ShoppingListsRepo:
  def addIngredients(ingredients: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit]

  def getIngredients: ZIO[AuthenticatedUser, DbError, Vector[IngredientId]]

  def deleteIngredients(ingredients: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit]

private final case class ShoppingListsLive(xa: Transactor)
  extends Repo[DbShoppingList, DbShoppingList, Null] with ShoppingListsRepo:

  override def addIngredients(ingredients: Seq[IngredientId]):
    ZIO[AuthenticatedUser, DbError, Unit] = for
    ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    _ <- xa.transact {
        sql"""
           INSERT INTO $shoppingListTable (${shoppingListTable.ownerId}, ${shoppingListTable.ingredientId})
           SELECT $ownerId, unnest(${ingredients.toArray})
           ON CONFLICT DO NOTHING
         """.update.run()
    }.mapError(handleDbError)
  yield ()

  override def getIngredients: ZIO[AuthenticatedUser, DbError, Vector[IngredientId]] = for
    ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    ingredientIds <- xa.transact {
      sql"""
        SELECT ${shoppingListTable.ingredientId} FROM $shoppingListTable
        WHERE ${shoppingListTable.ownerId} = $ownerId
      """.query[IngredientId].run()
    }.mapError(handleDbError)
  yield ingredientIds

  override def deleteIngredients(ingredients: Seq[IngredientId]):
    ZIO[AuthenticatedUser, DbError, Unit] = for
    ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    _ <- xa.transact {
      sql"""
        DELETE FROM $shoppingListTable
        WHERE ${shoppingListTable.ownerId} = $ownerId
        AND ${shoppingListTable.ingredientId} = ANY(${ingredients.toArray})
      """.update.run()
    }.mapError(handleDbError)
  yield ()

object ShoppingListsRepo:
  val layer = ZLayer.fromFunction(ShoppingListsLive(_))
