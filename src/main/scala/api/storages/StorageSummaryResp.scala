package api.storages

import db.tables.DbStorage
import domain.StorageId

private final case class StorageSummaryResp(id: StorageId, name: String)

private def dbToResp(dbStorage: DbStorage): StorageSummaryResp =
  StorageSummaryResp(dbStorage.id, dbStorage.name)
