package db.repositories

import db.tables.{DbStorage, DbStorageCreator, storageMembersTable, storagesTable}
import domain.StorageError.NotFound
import domain.DbError.{
  FailedDbQuery,
  DbNotRespondingError,
  UnexpectedDbError
}
import domain.{StorageId, UserId}
import db.{handleDbError, handleUnfailableQuery}

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

  override def createEmpty(name: String, ownerId: UserId): IO[UnexpectedDbError | DbNotRespondingError, StorageId] =
    xa.transact {
      val storage = insertReturning(DbStorageCreator(name, ownerId))
      storage.id
    }.mapError {
      handleDbError(_) match
        case FailedDbQuery(exc) => UnexpectedDbError(exc.getMessage)
        case error: (UnexpectedDbError | DbNotRespondingError) => error
    }

  override def getById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Option[DbStorage]] =
    xa.transact {
      findById(id)
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

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

  override def removeById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

object StoragesRepoLive:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
