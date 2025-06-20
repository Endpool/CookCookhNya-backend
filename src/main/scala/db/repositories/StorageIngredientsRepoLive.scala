package db.repositories

import db.tables.StorageIngredients
import domain.{IngredientId, StorageError, IngredientError, StorageId}

import com.augustnagro.magnum.magzio.*
import zio.{RLayer, Task, IO, ZIO, ZLayer}

trait StorageIngredientsRepo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[StorageError.NotFound, Vector[IngredientId]]

final case class StorageIngredientsRepoLive(xa: Transactor) extends Repo[StorageIngredients, StorageIngredients, Null]
  with StorageIngredientsRepo:

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit] =
    xa.transact(insert(StorageIngredients(storageId, ingredientId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit] =
    xa.transact(delete(StorageIngredients(storageId, ingredientId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def getAllIngredientsFromStorage(storageId: StorageId):
    IO[StorageError.NotFound, Vector[IngredientId]] =
    xa.transact {
      val s =
        sql"""
          select ${StorageIngredients.table.ingredientId} from ${StorageIngredients.table}
          where ${StorageIngredients.table.storageId} = $storageId
        """
      s.query[IngredientId].run()
    }.catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

object StorageIngredientsRepoLive:
  val layer: RLayer[Transactor, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive(_))
