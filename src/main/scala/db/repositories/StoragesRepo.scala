package db.repositories

import db.tables.{DbStorage, DbStorageCreator, storageMembersTable, storagesTable}
import db.{DbError, handleDbError}
import domain.{StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StoragesRepo:
  def createEmpty(name: String, ownerId: UserId): IO[DbError, StorageId]
  def removeById(id: StorageId): IO[DbError, Unit]
  def getById(id: StorageId): IO[DbError, Option[DbStorage]]
  def getAll(id: UserId) : IO[DbError, Vector[DbStorage]]

private final case class StoragesRepoLive(xa: Transactor)
  extends Repo[DbStorageCreator, DbStorage, StorageId] with StoragesRepo:

  override def createEmpty(name: String, ownerId: UserId): IO[DbError, StorageId] =
    xa.transact {
      val storage = insertReturning(DbStorageCreator(name, ownerId))
      storage.id
    }.mapError(handleDbError)

  override def getById(id: StorageId): IO[DbError, Option[DbStorage]] =
    xa.transact {
      findById(id)
    }.mapError(handleDbError)

  override def getAll(id: UserId): IO[DbError, Vector[DbStorage]] =
    xa.transact {
      sql"""
        SELECT DISTINCT ${storagesTable.id}, ${storagesTable.ownerId}, ${storagesTable.name}
        FROM $storagesTable LEFT JOIN $storageMembersTable
        ON ${storagesTable.id} = ${storageMembersTable.storageId}
        WHERE $id = ${storagesTable.ownerId}
           OR $id = ${storageMembersTable.memberId}
      """.query[DbStorage].run()
    }.mapError(handleDbError)

  override def removeById(id: StorageId): IO[DbError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(handleDbError)

object StoragesRepo:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
