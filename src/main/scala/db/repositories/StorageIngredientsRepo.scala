package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import domain.{IngredientId, StorageError, StorageId, UserId}
import db.{DbError, handleDbError}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer, IO, ZIO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]]

private final case class StorageIngredientsRepoLive(xa: Transactor)
  extends Repo[DbStorageIngredient, DbStorageIngredient, (StorageId, UserId)] with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
           insert into ${storageIngredientsTable}
           values ($storageId, $ingredientId)
           on conflict do nothing
         """.update.run()
      ()
    }.mapError(handleDbError)

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
        DELETE FROM $storageIngredientsTable
        WHERE ${storageIngredientsTable.storageId} = $storageId
          AND ${storageIngredientsTable.ingredientId} = $ingredientId
      """.update.run()
      ()
    }.mapError(handleDbError)

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
