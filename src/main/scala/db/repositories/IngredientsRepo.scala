package db.repositories

import db.tables.{DbIngredient, DbIngredientCreator}
import domain.{IngredientId, UserId}
import db.{DbError, handleDbError}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IngredientsRepo:
  def add(name: String): IO[DbError, DbIngredient]
  def getById(id: IngredientId): IO[DbError, Option[DbIngredient]]
  def removeById(id: IngredientId): IO[DbError, Unit]
  def getAll(userId: UserId):
  ZIO[StorageMembersRepo & StorageIngredientsRepo, DbError, Vector[DbIngredient]] 

private final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:
  override def add(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(name))).mapError {
      handleDbError
    }

  override def getById(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    xa.transact(findById(id)).mapError {
      handleDbError
    }

  override def removeById(id: IngredientId): IO[DbError, Unit] =
    xa.transact(deleteById(id)).mapError {
      handleDbError
    }

  override def getAll(userId: UserId):
    ZIO[StorageMembersRepo & StorageIngredientsRepo, DbError, Vector[DbIngredient]] =
    for 
      userStorageIds <- ZIO.serviceWithZIO[StorageMembersRepo](_.getAllUserStorages(userId))
      userIngredientIds <- ZIO.foreach(userStorageIds) {
        storageId => ZIO.serviceWithZIO[StorageIngredientsRepo](_.getAllIngredientsFromStorage(storageId))
      }.map(_.flatten)
      userIngredients <- xa.transact {
        userIngredientIds.map(findById).flatten
      }.mapError(handleDbError) 

    yield userIngredients
      
object IngredientsRepoLive:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
