package db.tables

import com.augustnagro.magnum.*

import domain.{StorageId, UserId, StorageView}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Storages(
                         @Id id: StorageId,
                         ownerId: UserId,
                         name: String
                         ) derives DbCodec

object Storages:
  val table = TableInfo[Storages, Storages, StorageId]
  def toDomain(storage: Storages): StorageView = storage match
    case Storages(id, ownerId, name) => StorageView(id, name, ownerId)
