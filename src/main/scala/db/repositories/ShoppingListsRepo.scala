package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbShoppingList, shoppingListTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, UserId}
import db.QuillConfig.provideDS

import javax.sql.DataSource
import com.augustnagro.magnum.magzio.*
import io.getquill.*
import db.QuillConfig.ctx.*
import db.repositories.ShoppingListsQueries.*
import zio.{ZIO, ZLayer}

trait ShoppingListsRepo:
  def addIngredient(ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def addIngredients(ingredientIds: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit]

  def getIngredients: ZIO[AuthenticatedUser, DbError, Vector[IngredientId]]

  def deleteIngredient(ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def deleteIngredients(ingredientIds: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit]

private final case class ShoppingListsLive(xa: Transactor, dataSource: DataSource)
  extends Repo[DbShoppingList, DbShoppingList, Null] with ShoppingListsRepo:

  private given DataSource = dataSource
  def addIngredient(ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    for 
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      res <- run(addIngredientQ(lift(ownerId), lift(ingredientId))).unit.provideDS
    yield res 

  override def addIngredients(ingredientIds: Seq[IngredientId]):
    ZIO[AuthenticatedUser, DbError, Unit] = ZIO.foreachParDiscard(ingredientIds)(addIngredient) 

  override def getIngredients: ZIO[AuthenticatedUser, DbError, List[IngredientId]] = for
    ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    res <- run(getIngredientsQ(lift(ownerId))).provideDS
  yield res

  override def deleteIngredient(ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    run(deleteIngredientQ(lift(ingredientId))).unit.provideDS

  override def deleteIngredients(ingredientIds: Seq[IngredientId]):
    ZIO[AuthenticatedUser, DbError, Unit] = ZIO.foreachParDiscard(ingredientIds)(deleteIngredient)

object ShoppingListsQueries:
  inline def addIngredientQ(inline userId: UserId, inline ingredientId: IngredientId): Unit =
    query[DbShoppingList]
      .insertValue(DbShoppingList(userId, ingredientId))
      
  inline def deleteIngredientQ(inline ingredientId: IngredientId): Delete[DbShoppingList] =
    query[DbShoppingList]
      .filter(_.ingredientId == ingredientId)
      .delete

  inline def getIngredientsQ(inline ownerId: UserId): EntityQuery[IngredientId] =
    query[DbShoppingList]
      .filter(_.ownerId == ownerId)
      .map(_.ingredientId)

object ShoppingListsRepo:
  val layer = ZLayer.fromFunction(ShoppingListsLive.apply)
