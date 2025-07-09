package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbStorage, DbStorageCreator, storageMembersTable, storagesTable}
import db.{DbError, handleDbError}
import domain.{StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StoragesRepo:
  def createEmpty(name: String): ZIO[AuthenticatedUser, DbError, StorageId]
  def removeById(storageId: StorageId): ZIO[AuthenticatedUser, DbError, Unit]
  def getById(storageId: StorageId): ZIO[AuthenticatedUser, DbError, Option[DbStorage]]
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

  override def getById(storageId: StorageId): ZIO[AuthenticatedUser, DbError, Option[DbStorage]] = for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    mStorage <- xa.transact {
      sql"""
        SELECT DISTINCT ${storagesTable.id}, ${storagesTable.ownerId}, ${storagesTable.name}
        FROM $storagesTable LEFT JOIN $storageMembersTable
        ON ${storagesTable.id} = ${storageMembersTable.storageId}
        WHERE $storageId = ${storagesTable.id}
          AND (  $userId = ${storagesTable.ownerId}
              OR $userId = ${storageMembersTable.memberId}
              )
      """.query[DbStorage].run().headOption
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

  override def removeById(storageId: StorageId): ZIO[AuthenticatedUser, DbError, Unit] = for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    _ <- xa.transact {
      sql"""
        DELETE FROM $storagesTable
        WHERE $storageId = ${storagesTable.id}
          AND $userId = ${storagesTable.ownerId}
      """.update.run()
    }.mapError(handleDbError)
  yield ()

object StoragesRepo:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
