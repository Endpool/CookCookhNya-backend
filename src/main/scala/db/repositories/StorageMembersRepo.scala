package db.repositories

import db.tables.{DbStorageMember, storageMembersTable}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import domain.{UserId, StorageId}
import db.{DbError, handleDbError}

import com.augustnagro.magnum.magzio.*
import zio.{ZIO, IO, RLayer, ZLayer}

trait StorageMembersRepo:
  def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def getAllStorageMembers(storageId: StorageId):
    IO[DbError, Vector[UserId]]

private final case class StorageMembersRepoLive(xa: Transactor)
  extends Repo[DbStorageMember, DbStorageMember, Null]
  with StorageMembersRepo:

  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact(insert(DbStorageMember(storageId, memberId))).mapError {
      handleDbError
    }

  override def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
       delete from $storageMembersTable
       where ${storageMembersTable.storageId} = $storageId
       and ${storageMembersTable.memberId} = $memberId
         """.update.run()
      ()
    }.mapError(handleDbError)

  override def getAllStorageMembers(storageId: StorageId):
    IO[DbError, Vector[UserId]] =
    xa.transact {
      sql"""
        SELECT ${storageMembersTable.memberId} FROM ${storageMembersTable}
        WHERE ${storageMembersTable.storageId} = $storageId
      """.query[UserId].run()
    }.mapError(handleDbError)

object StorageMembersRepoLive:
  val layer: RLayer[Transactor, StorageMembersRepo] =
    ZLayer.fromFunction(StorageMembersRepoLive(_))
