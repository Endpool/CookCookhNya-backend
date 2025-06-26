package db.repositories

import db.tables.{DbStorage, DbStorageCreator, storageMembersTable, storagesTable}
import domain.StorageError.NotFound
import domain.{Storage, StorageError, StorageId, UserId}
import domain.{StorageId, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}
import domain.DbError

trait StoragesRepo:
  def createEmpty(name: String, ownerId: UserId): IO[DbError.UnexpectedDbError, StorageId]
  def removeById(id: StorageId): IO[DbError.UnexpectedDbError, Unit]
  def getById(id: StorageId): IO[DbError.UnexpectedDbError, Option[DbStorage]]
  def getAll(id: UserId) : UIO[Vector[DbStorage]]

private final case class StoragesRepoLive(xa: Transactor)
  extends Repo[DbStorageCreator, DbStorage, StorageId] with StoragesRepo:

  override def createEmpty(name: String, ownerId: UserId): IO[DbError.UnexpectedDbError, StorageId] =
    xa.transact {
      val storage = insertReturning(DbStorageCreator(name, ownerId))
      storage.id
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

  override def getById(id: StorageId): IO[DbError.UnexpectedDbError, Option[DbStorage]] =
    xa.transact {
      findById(id)
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

  override def getAll(id: UserId): UIO[Vector[DbStorage]] =
    xa.transact {
      sql"""
           select distinct ${storagesTable.id}, ${storagesTable.ownerId}, ${storagesTable.name}
           from $storagesTable left join $storageMembersTable
           on ${storagesTable.id} = ${storageMembersTable.storageId}
           where $id = ${storagesTable.ownerId} or $id = ${storageMembersTable.memberId}
         """
        .query[DbStorage].run()
    }.catchAll(_ => ZIO.succeed(Vector.empty))

  override def removeById(id: StorageId): IO[DbError.UnexpectedDbError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

object StoragesRepoLive:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
