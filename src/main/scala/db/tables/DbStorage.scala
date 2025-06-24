package db.tables

import com.augustnagro.magnum.*

import domain.{StorageId, UserId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class DbStorage(
  @Id id: StorageId,
  ownerId: UserId,
  name: String
) derives DbCodec

case class DbStorageCreator(name: String, ownerId: UserId)

val storagesTable = TableInfo[DbStorageCreator, DbStorage, StorageId]
