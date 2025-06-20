package api.db.repositories

import api.db.tables.StorageIngredients
import api.domain.{IngredientId, StorageError, IngredientError, StorageId}
import com.augustnagro.magnum.magzio.*
import zio.{RLayer, Task, IO, ZIO, ZLayer}

trait IStorageIngredientsReopo:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError.NotFound | IngredientError.NotFound, Unit]

  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[StorageError.NotFound, Vector[IngredientId]]

final case class StorageIngredientsRepo(xa: Transactor) extends Repo[StorageIngredients, StorageIngredients, Null]
  with IStorageIngredientsReopo:
  
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

object StorageIngredientsRepo:
  val layer: RLayer[Transactor, IStorageIngredientsReopo] =
    ZLayer.fromFunction(StorageIngredientsRepo(_))
