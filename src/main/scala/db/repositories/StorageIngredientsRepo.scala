package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer, IO, ZIO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]]

  def inStorage(storageId: StorageId, ingredientId: IngredientId): IO[DbError, Boolean]

private final case class StorageIngredientsRepoLive(xa: Transactor)
  extends Repo[DbStorageIngredient, DbStorageIngredient, (StorageId, UserId)] with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
        INSERT INTO ${storageIngredientsTable}
        VALUES ($storageId, $ingredientId)
        ON CONFLICT DO NOTHING
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
    }.mapError(handleDbError)

  override def inStorage(storageId: StorageId, ingredientId: IngredientId): IO[DbError, Boolean] =
    xa.transact {
      val spec = Spec[DbStorageIngredient]
        .where(sql"${storageIngredientsTable.storageId} = $storageId")
        .where(sql"${storageIngredientsTable.ingredientId} = $ingredientId")
        .limit(1)
      val exists = ! findAll(spec).isEmpty
      exists
    }.mapError(handleDbError)

object StorageIngredientsRepo:
  val layer: RLayer[Transactor, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive(_))
