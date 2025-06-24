package api.storages

import db.tables.DbStorage
import domain.{StorageId, UserId}

private final case class StorageSummaryResp(id: StorageId, ownerId: UserId, name: String)

private def dbToResp(dbStorage: DbStorage): StorageSummaryResp =
  StorageSummaryResp(dbStorage.id, dbStorage.ownerId, dbStorage.name)
