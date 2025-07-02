package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import domain.{IngredientId, StorageError, IngredientError, StorageId, UserId, DbError}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer,  IO, ZLayer}
import zio.ZIO
import db.tables.storageMembersTable

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError.UnexpectedDbError, Vector[IngredientId]]

private final case class StorageIngredientsRepoLive(xa: Transactor)
  extends Repo[DbStorageIngredient, DbStorageIngredient, (StorageId, UserId)] with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit] =
    xa.transact(insert(DbStorageIngredient(storageId, ingredientId))).mapError {
      _ => StorageError.NotFound(storageId) // TODO actual error handling
    }

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound, Unit] =
    xa.transact {
      sql"""
        DELETE FROM $storageIngredientsTable
        WHERE ${storageIngredientsTable.storageId} = $storageId
          AND ${storageIngredientsTable.ingredientId} = $ingredientId
      """.update.run()
      ()
    }.mapError {
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

object StorageIngredientsRepo:
  val layer: RLayer[Transactor, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive(_))
