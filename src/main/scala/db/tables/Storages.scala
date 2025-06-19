package db.tables

import com.augustnagro.magnum.*

import domain.{StorageId, Storage, UserId, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Storages(
                         @Id storageId: StorageId,
                         ownerId: UserId,
                         ) derives DbCodec

object Storages:
  val table = TableInfo[Storages, Storages, StorageId]
