package db.repositories

import db.tables.DbStorageIngredient
import db.DbError
import domain.{IngredientId, StorageId}

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

private final case class StorageIngredientsRepoLive(dataSource: DataSource)
  extends StorageIngredientsRepo:

  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import StorageIngredientsQueries.*

  private given DataSource = dataSource

  override def addIngredientToStorage(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    run(
      storageIngredientsQ
        .insertValue(lift(DbStorageIngredient(storageId, ingredientId)))
        .onConflictIgnore
    ).unit.provideDS

  override def removeIngredientFromStorageById(storageId: StorageId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    run(
      storageIngredientsQ
        .filter(si => si.storageId    == lift(storageId)
                   && si.ingredientId == lift(ingredientId))
        .delete
    ).unit.provideDS

  override def removeIngredientsFromStorage(storageId: StorageId, ingredientIds: Vector[IngredientId]):
    IO[DbError, Unit] =
    run(
      storageIngredientsQ
        .filter(si => si.storageId == lift(storageId)
                   && liftQuery(ingredientIds).contains(si.ingredientId))
        .delete
    ).unit.provideDS

  override def getAllIngredientsFromStorage(storageId: StorageId):
    IO[DbError, Vector[IngredientId]] =
    run(
      storageIngredientsQ
        .filter(_.storageId == lift(storageId))
        .map(_.ingredientId)
    ).map(Vector.from).provideDS

  override def inStorage(storageId: StorageId, ingredientId: IngredientId): IO[DbError, Boolean] =
    run(inStorageQ(lift(storageId), lift(ingredientId))).provideDS

object StorageIngredientsQueries:
  inline def storageIngredientsQ: EntityQuery[DbStorageIngredient] =
    query[DbStorageIngredient]

  inline def inStorageQ(inline storageId: StorageId, inline ingredientId: IngredientId) =
    storageIngredientsQ
      .filter(si => si.storageId == storageId && si.ingredientId == ingredientId)
      .map(_ => 1)
      .nonEmpty

object StorageIngredientsRepo:
  val layer: RLayer[DataSource, StorageIngredientsRepo] =
    ZLayer.fromFunction(StorageIngredientsRepoLive.apply)
