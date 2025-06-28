package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import domain.{IngredientId, StorageError, StorageId, DbError}
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}
import db.{handleDbError, handleUnfailableQuery}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer, IO, ZIO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]]

private final case class StorageIngredientsRepoLive(xa: Transactor)
  extends Repo[DbStorageIngredient, DbStorageIngredient, (StorageId, UserId)] with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    xa.transact(insert(DbStorageIngredient(storageId, ingredientId))).mapError {
      handleDbError
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
    IO[DbError, Vector[IngredientId]] =
    xa.transact {
      sql"""
        SELECT ${storageIngredientsTable.ingredientId} FROM ${storageIngredientsTable}
        WHERE ${storageIngredientsTable.storageId} = $storageId
      """.query[IngredientId].run()
    }.mapError {
      handleDbError
    }

object StorageIngredientsRepoLive:
  val layer: RLayer[Transactor, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive(_))
