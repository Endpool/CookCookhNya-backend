package api.storages

import db.tables.DbStorage
import domain.StorageId

final case class StorageSummaryResp(id: StorageId, name: String)

object StorageSummaryResp:
  def fromDb(dbStorage: DbStorage): StorageSummaryResp =
    StorageSummaryResp(dbStorage.id, dbStorage.name)
