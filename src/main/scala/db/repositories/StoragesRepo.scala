package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbStorage, DbStorageCreator, storageMembersTable, storagesTable}
import db.{DbError, handleDbError}
import domain.{StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StoragesRepo:
  def createEmpty(name: String): ZIO[AuthenticatedUser, DbError, StorageId]
  def removeById(id: StorageId): ZIO[AuthenticatedUser, DbError, Unit]
  def getById(id: StorageId): ZIO[AuthenticatedUser, DbError, Option[DbStorage]]
  def getAll : ZIO[AuthenticatedUser, DbError, Vector[DbStorage]]

private final case class StoragesRepoLive(xa: Transactor)
  extends Repo[DbStorageCreator, DbStorage, StorageId] with StoragesRepo:

  override def createEmpty(name: String): ZIO[AuthenticatedUser, DbError, StorageId] = for
    creatorId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    storageId <- xa.transact {
      val storage = insertReturning(DbStorageCreator(name, creatorId))
      storage.id
    }.mapError(handleDbError)
  yield storageId

  //TODO validate user
  override def getById(id: StorageId): ZIO[AuthenticatedUser, DbError, Option[DbStorage]] = for
    mStorage <- xa.transact {
      findById(id)
    }.mapError(handleDbError)
  yield mStorage

  override def getAll: ZIO[AuthenticatedUser, DbError, Vector[DbStorage]] = for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    storages <- xa.transact {
      sql"""
        SELECT DISTINCT ${storagesTable.id}, ${storagesTable.ownerId}, ${storagesTable.name}
        FROM $storagesTable LEFT JOIN $storageMembersTable
        ON ${storagesTable.id} = ${storageMembersTable.storageId}
        WHERE $userId = ${storagesTable.ownerId}
           OR $userId = ${storageMembersTable.memberId}
      """.query[DbStorage].run()
    }.mapError(handleDbError)
  yield storages

  //TODO validate user
  override def removeById(id: StorageId): ZIO[AuthenticatedUser, DbError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(handleDbError)

object StoragesRepo:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
