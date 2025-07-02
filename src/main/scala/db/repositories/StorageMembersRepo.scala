package db.repositories

import db.tables.{DbStorageMember, storageMembersTable, storagesTable}
import db.DbError
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

  def getAllUserStorageIds(userId: UserId):
    IO[DbError, Vector[StorageId]]
  
private final case class StorageMembersRepoLive(xa: Transactor)
  extends Repo[DbStorageMember, DbStorageMember, Null]
  with StorageMembersRepo:

  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
           insert into ${storageMembersTable} (${storageMembersTable.storageId}, ${storageMembersTable.memberId})
           values ($storageId, $memberId)
           on conflict (${storageMembersTable.storageId}, ${storageMembersTable.memberId})
           do nothing
         """.update.run()
      ()
    }.mapError(handleDbError)

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

  override def getAllUserStorageIds(userId: UserId):
    IO[DbError, Vector[StorageId]] =
    xa.transact {
      sql"""
        SELECT ${storageMembersTable.storageId} FROM $storageMembersTable
        WHERE ${storageMembersTable.memberId} = $userId

        UNION

        SELECT ${storagesTable.id}
        FROM $storagesTable
        WHERE ${storagesTable.ownerId} = $userId
      """.query[UserId].run()
    }.mapError(handleDbError)

object StorageMembersRepoLive:
  val layer: RLayer[Transactor, StorageMembersRepo] =
    ZLayer.fromFunction(StorageMembersRepoLive(_))
