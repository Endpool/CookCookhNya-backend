package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import domain.{IngredientId, StorageError, IngredientError, StorageId, DbError}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer, Task, IO, ZIO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError.UnexpectedDbError, Vector[IngredientId]]

final case class StorageIngredientsRepoLive(xa: Transactor)
  extends Repo[DbStorageIngredient, DbStorageIngredient, Null]
  with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit] =
    xa.transact(insert(DbStorageIngredient(storageId, ingredientId))).mapError {
      _ => StorageError.NotFound(storageId) // TODO actual error handling
    }

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound, Unit] =
    xa.transact(delete(DbStorageIngredient(storageId, ingredientId))).mapError {
      _ => StorageError.NotFound(storageId) // TODO actual error handling
    }

  override def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError.UnexpectedDbError, Vector[IngredientId]] =
    xa.transact {
      sql"""
        SELECT ${storageIngredientsTable.ingredientId} FROM ${storageIngredientsTable}
        WHERE ${storageIngredientsTable.storageId} = $storageId
      """.query[IngredientId].run()
    }.mapError {
      e => DbError.UnexpectedDbError(e.getMessage()) // TODO actual error handling
    }

object StorageIngredientsRepoLive:
  val layer: RLayer[Transactor, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive(_))
