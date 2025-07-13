package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbShoppingList, shoppingListTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, IngredientNotFound, StorageId, UserId}
import db.QuillConfig.provideDS
import db.repositories.StorageIngredientsQueries.addIngredientToStorageQ

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

  def deleteIngredient(userId: UserId, ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def deleteIngredients(ingredientIds: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit]

  def buyIngredientsToStorage(ingredientIds: Seq[IngredientId], storageId: StorageId):
    ZIO[AuthenticatedUser, DbError, Unit]

private final case class ShoppingListsLive(dataSource: DataSource)
  extends Repo[DbShoppingList, DbShoppingList, Null] with ShoppingListsRepo:

  private given DataSource = dataSource
  def addIngredient(ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      res <- run(addIngredientQ(lift(ownerId), lift(ingredientId))).unit.provideDS
    yield res

  override def addIngredients(ingredientIds: Seq[IngredientId]):ZIO[AuthenticatedUser, DbError, Unit] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      _ <- run(liftQuery(ingredientIds).foreach(
        addIngredientQ(lift(ownerId), _)
      )).provideDS
    yield ()

  override def getIngredients: ZIO[AuthenticatedUser, DbError, Vector[IngredientId]] = for
    ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    res <- run(getIngredientsQ(lift(ownerId))).provideDS
  yield res.toVector

  override def deleteIngredient(userId: UserId, ingredientId: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    run(deleteIngredientQ(lift(userId), lift(ingredientId))).unit.provideDS

  def deleteIngredients(ingredientIds: Seq[IngredientId]): ZIO[AuthenticatedUser, DbError, Unit] =
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      _ <- run(liftQuery(ingredientIds).foreach(deleteIngredientQ(lift(userId), _))).unit.provideDS
    yield ()

  override def buyIngredientsToStorage(ingredientIds: Seq[IngredientId], storageId: StorageId):
    ZIO[AuthenticatedUser, DbError, Unit] = transaction {
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      _ <- run(liftQuery(ingredientIds).foreach(deleteIngredientQ(lift(userId), _)))
      _ <- run(liftQuery(ingredientIds).foreach(addIngredientToStorageQ(lift(storageId), _)))
    yield ()
  }.provideDS

object ShoppingListsQueries:
  inline def addIngredientQ(inline userId: UserId, inline ingredientId: IngredientId) =
    query[DbShoppingList]
      .insertValue(DbShoppingList(userId, ingredientId))

  inline def deleteIngredientQ(inline userId: UserId, inline ingredientId: IngredientId): Delete[DbShoppingList] =
    query[DbShoppingList]
      .filter(sl => sl.ownerId == userId && sl.ingredientId == ingredientId)
      .delete

  inline def getIngredientsQ(inline ownerId: UserId): EntityQuery[IngredientId] =
    query[DbShoppingList]
      .filter(_.ownerId == ownerId)
      .map(_.ingredientId)

object ShoppingListsRepo:
  val layer = ZLayer.fromFunction(ShoppingListsLive.apply)
