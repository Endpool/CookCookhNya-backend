package api.storages

import db.repositories.StorageMembersRepo
import db.tables.DbStorage
import domain.{InternalServerError, UserId}
import zio.ZIO

def checkForMembership(userId: UserId, storage: DbStorage):
ZIO[StorageMembersRepo, InternalServerError, Boolean] =
  if userId == storage.ownerId then ZIO.succeed(true)
  else ZIO.serviceWithZIO[StorageMembersRepo] {
    _.getAllStorageMembers(storage.id)
  }.flatMap(members => ZIO.succeed(members.contains(userId)))
    .mapError(_ => InternalServerError())
