package api.db.tables

import com.augustnagro.magnum.*
import api.domain.{IngredientId, StorageId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class StorageIngredients(
                              storageId: StorageId,
                              ingredientId: IngredientId
                             ) derives DbCodec
object StorageIngredients:
  val table = TableInfo[StorageIngredients, StorageIngredients, StorageId & IngredientId]