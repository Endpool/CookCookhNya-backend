package api.storages

import api.Authentication.AuthenticatedUser
import db.repositories.StorageMembersRepo
import db.tables.DbStorage
import domain.{InternalServerError, UserId}

import zio.ZIO

def checkForMembership(storage: DbStorage):
  ZIO[AuthenticatedUser & StorageMembersRepo, InternalServerError, Boolean] = for
  userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
  res <- if userId == storage.ownerId
    then ZIO.succeed(true)
    else ZIO.serviceWithZIO[StorageMembersRepo] {
      _.getAllStorageMembers(storage.id)
    }.map(_.contains(userId))
      .mapError(_ => InternalServerError())
yield res
