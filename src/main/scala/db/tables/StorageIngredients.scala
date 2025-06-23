package db.tables

import com.augustnagro.magnum.*
import domain.{IngredientId, StorageId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class DbStorageIngredient(
  storageId: StorageId,
  ingredientId: IngredientId
) derives DbCodec

val storageIngredientsTable = TableInfo[DbStorageIngredient, DbStorageIngredient, StorageId & IngredientId]
