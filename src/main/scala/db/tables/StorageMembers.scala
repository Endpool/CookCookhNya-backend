package db.tables

import com.augustnagro.magnum.*
import domain.{UserId, StorageId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class StorageMembers(
                         storageId: StorageId,
                         memberId: UserId
                         ) derives DbCodec

object StorageMembers:
  val table = TableInfo[StorageMembers, StorageMembers, StorageId & UserId]
