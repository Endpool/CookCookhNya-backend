package db.repositories

import db.tables.{DbStorageIngredient, storageIngredientsTable}
import db.{DbError, handleDbError}
import domain.{IngredientId, StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import io.getquill.*
import javax.sql.DataSource
import zio.{RLayer, IO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit]

  def removeIngredientsFromStorage(storageId: StorageId, ingredientIds: Vector[IngredientId]):
    IO[DbError, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]]

  def inStorage(storageId: StorageId, ingredientId: IngredientId): IO[DbError, Boolean]

private final case class StorageIngredientsRepoLive(xa: Transactor, dataSource: DataSource)
  extends Repo[DbStorageIngredient, DbStorageIngredient, (StorageId, UserId)] with StorageIngredientsRepo:

  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import StorageIngredientsQueries.*

  private given DataSource = dataSource

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
        INSERT INTO $storageIngredientsTable
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

  override def removeIngredientsFromStorage(storageId: StorageId, ingredientIds: Vector[IngredientId]):
    IO[DbError, Unit] = {
    xa.transact {
      sql"""
        DELETE FROM $storageIngredientsTable
        WHERE ${storageIngredientsTable.storageId} = $storageId
        AND ${storageIngredientsTable.ingredientId} = ANY(${ingredientIds.toArray})
      """.update.run()
      ()
    }.mapError(handleDbError)
  }

  override def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]] =
    xa.transact {
      sql"""
        SELECT ${storageIngredientsTable.ingredientId} FROM $storageIngredientsTable
        WHERE ${storageIngredientsTable.storageId} = $storageId
      """.query[IngredientId].run()
    }.mapError(handleDbError)

  override def inStorage(storageId: StorageId, ingredientId: IngredientId): IO[DbError, Boolean] =
    run(inStorageQ(lift(storageId), lift(ingredientId))).provideDS

object StorageIngredientsQueries:
  inline def inStorageQ(inline storageId: StorageId, inline ingredientId: IngredientId) =
    query[DbStorageIngredient]
      .filter(si => si.storageId == storageId && si.ingredientId == ingredientId)
      .map(_ => 1)
      .nonEmpty

object StorageIngredientsRepo:
  val layer: RLayer[Transactor & DataSource, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive.apply)
