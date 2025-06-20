package api.db.tables

import com.augustnagro.magnum.*

import api.domain.{StorageId, Storage, UserId, IngredientId, StorageView}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Storages(
                         @Id storageId: StorageId,
                         ownerId: UserId,
                         name: String
                         ) derives DbCodec

object Storages:
  val table = TableInfo[Storages, Storages, StorageId]
  def toDomain(storage: Storages): StorageView = storage match
    case Storages(storageId, ownerId, name) => StorageView(storageId, name, ownerId)

final case class StorageCreationEntity(name: String, ownerId: UserId)
