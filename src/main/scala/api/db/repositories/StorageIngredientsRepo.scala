package api.db.repositories

import api.db.tables.StorageIngredients
import api.domain.{IngredientId, StorageError, IngredientError, StorageId}
import com.augustnagro.magnum.magzio.*
import zio.{Exit, RLayer, Task, IO, ZIO, ZLayer}

trait StorageIngredientsRepoInterface:
  def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId): IO[StorageError | IngredientError, Unit]
  def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[StorageError | IngredientError, Unit]
  def getAllIngredientsFromStorage(storageId: StorageId):
    IO[StorageError, Vector[IngredientId]]

final case class StorageIngredientsRepo(xa: Transactor) extends Repo[StorageIngredients, StorageIngredients, Null]
  with StorageIngredientsRepoInterface:
  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
  IO[StorageError | IngredientError, Unit] =
    xa.transact(insert(StorageIngredients(storageId, ingredientId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
  IO[StorageError | IngredientError, Unit] =
    xa.transact(delete(StorageIngredients(storageId, ingredientId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def getAllIngredientsFromStorage(storageId: StorageId):
  IO[StorageError, Vector[IngredientId]] =
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
  val layer: RLayer[Transactor, StorageIngredientsRepoInterface] =
    ZLayer.fromFunction(StorageIngredientsRepo(_))
