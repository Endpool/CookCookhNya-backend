package api.storages

import db.tables.DbStorage
import domain.{StorageId, UserId}

final case class StorageSummaryResp(id: StorageId, ownerId: UserId, name: String)

object StorageSummaryResp:
  def fromDb(dbStorage: DbStorage): StorageSummaryResp =
    StorageSummaryResp(dbStorage.id, dbStorage.ownerId, dbStorage.name)
