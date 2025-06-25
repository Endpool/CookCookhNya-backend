package db.tables

import db.CustomSqlNameMapper
import domain.{IngredientId, StorageId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
case class DbStorageIngredient(
  storageId: StorageId,
  ingredientId: IngredientId
) derives DbCodec

val storageIngredientsTable = TableInfo[DbStorageIngredient, DbStorageIngredient, StorageId & IngredientId]
