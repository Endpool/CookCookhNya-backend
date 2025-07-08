package db.tables

import db.CustomSqlNameMapper
import domain.StorageId

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
case class DbStorageInvitation(
  storageId: StorageId,
  invitation: String
) derives DbCodec

val storageInvitationTable = TableInfo[DbStorageInvitation, DbStorageInvitation, StorageId & String]

