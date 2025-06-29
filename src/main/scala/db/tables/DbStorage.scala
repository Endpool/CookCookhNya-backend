package db.tables

import com.augustnagro.magnum.*

import db.CustomSqlNameMapper
import domain.{StorageId, UserId}

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbStorage(
  @Id id: StorageId,
  ownerId: UserId,
  name: String
) derives DbCodec

case class DbStorageCreator(name: String, ownerId: UserId)

val storagesTable  = TableInfo[DbStorageCreator, DbStorage, StorageId]
