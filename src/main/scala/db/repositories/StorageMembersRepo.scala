package db.repositories

import db.tables.{DbStorageMember, storageMembersTable}
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}
import domain.{UserId, StorageId, DbError}
import db.{handleDbError, handleUnfailableQuery}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, ZLayer}

trait StorageMembersRepo:
  def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def getAllStorageMembers(storageId: StorageId):
    IO[UnexpectedDbError | DbNotRespondingError, Vector[UserId]]

final case class StorageMembersRepoLive(xa: Transactor)
  extends Repo[DbStorageMember, DbStorageMember, Null]
  with StorageMembersRepo:

  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact(insert(DbStorageMember(storageId, memberId))).mapError {
      handleDbError
    }

  override def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact(delete(DbStorageMember(storageId, memberId))).mapError {
      handleDbError
    }

  override def getAllStorageMembers(storageId: StorageId):
    IO[UnexpectedDbError | DbNotRespondingError, Vector[UserId]] =
    xa.transact {
      sql"""
        SELECT ${storageMembersTable.memberId} FROM ${storageMembersTable}
        WHERE ${storageMembersTable.storageId} = $storageId
      """.query[UserId].run()
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

object StorageMembersRepoLive:
  val layer: RLayer[Transactor, StorageMembersRepo] =
    ZLayer.fromFunction(StorageMembersRepoLive(_))
