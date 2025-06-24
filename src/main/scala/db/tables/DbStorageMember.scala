package db.tables

import db.CustomSqlNameMapper
import domain.{UserId, StorageId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
case class DbStorageMember(
  storageId: StorageId,
  memberId: UserId
) derives DbCodec

val storageMembersTable = TableInfo[DbStorageMember, DbStorageMember, StorageId & UserId]
