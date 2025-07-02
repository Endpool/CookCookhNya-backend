package db.repositories

import db.tables.{DbStorageMember, storageMembersTable}
import domain.*

import com.augustnagro.magnum.magzio.*
import zio.{ZIO, IO, RLayer, ZLayer}

trait StorageMembersRepo:
  def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[StorageError.NotFound | UserError.NotFound, Unit]
  def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[StorageError.NotFound | UserError.NotFound, Unit]
  def getAllStorageMembers(storageId: StorageId):
    IO[DbError.UnexpectedDbError, Vector[UserId]]

private final case class StorageMembersRepoLive(xa: Transactor)
  extends Repo[DbStorageMember, DbStorageMember, Null]
  with StorageMembersRepo:

  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[StorageError.NotFound | UserError.NotFound, Unit] =
    xa.transact(insert(DbStorageMember(storageId, memberId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[StorageError.NotFound | UserError.NotFound, Unit] =
    xa.transact(delete(DbStorageMember(storageId, memberId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def getAllStorageMembers(storageId: StorageId):
    IO[DbError.UnexpectedDbError, Vector[UserId]] =
    xa.transact {
      sql"""
        SELECT ${storageMembersTable.memberId} FROM ${storageMembersTable}
        WHERE ${storageMembersTable.storageId} = $storageId
      """.query[UserId].run()
    }.mapError{
      e => {
        println(e.getMessage)
        DbError.UnexpectedDbError(e.getMessage())
      }
    }

object StorageMembersRepo:
  val layer: RLayer[Transactor, StorageMembersRepo] =
    ZLayer.fromFunction(StorageMembersRepoLive(_))
