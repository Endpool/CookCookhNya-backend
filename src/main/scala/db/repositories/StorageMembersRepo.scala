package db.repositories

import db.tables.{DbStorageMember, storageMembersTable, storagesTable}
import db.{DbError, handleDbError}
import domain.{UserId, StorageId}

import com.augustnagro.magnum.magzio.*
import zio.{ZIO, IO, RLayer, ZLayer}
import api.Authentication.AuthenticatedUser

trait StorageMembersRepo:
  def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit]
  def getAllStorageMembers(storageId: StorageId):
    IO[DbError, Vector[UserId]]
  def getAllUserStorageIds:
    ZIO[AuthenticatedUser, DbError, Vector[StorageId]]
  def checkForMembership(storageId: StorageId):
    ZIO[AuthenticatedUser, DbError, Boolean]

private final case class StorageMembersRepoLive(xa: Transactor)
  extends Repo[DbStorageMember, DbStorageMember, Null]
  with StorageMembersRepo:

  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
        INSERT INTO ${storageMembersTable} (${storageMembersTable.storageId}, ${storageMembersTable.memberId})
        VALUES ($storageId, $memberId)
        ON CONFLICT (${storageMembersTable.storageId}, ${storageMembersTable.memberId})
        DO NOTHING
      """.update.run()
      ()
    }.mapError(handleDbError)

  override def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
    IO[DbError, Unit] =
    xa.transact {
      sql"""
        DELETE FROM $storageMembersTable
        WHERE ${storageMembersTable.storageId} = $storageId
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

  override def getAllUserStorageIds:
    ZIO[AuthenticatedUser, DbError, Vector[StorageId]] =
    ZIO.serviceWithZIO[AuthenticatedUser]{ authenticatedUser =>
      val userId = authenticatedUser.userId
      xa.transact {
        sql"""
          SELECT ${storageMembersTable.storageId} FROM $storageMembersTable
          WHERE ${storageMembersTable.memberId} = $userId
          UNION
          SELECT ${storagesTable.id} FROM $storagesTable
          WHERE ${storagesTable.ownerId} = $userId
        """.query[UserId].run()
      }.mapError(handleDbError)
    }

  override def checkForMembership(storageId: StorageId): ZIO[AuthenticatedUser, DbError, Boolean] =
    ZIO.serviceWithZIO[AuthenticatedUser]{ authenticatedUser =>
      val userId = authenticatedUser.userId
      xa.transact {
        sql"""
            SELECT 1
            FROM $storageMembersTable sm
            JOIN $storagesTable s
              ON sm.${storageMembersTable.storageId} = s.${storagesTable.id}
            WHERE sm.${storageMembersTable.storageId} = $storageId 
              AND(sm.${storageMembersTable.memberId} = $userId OR s.${storagesTable.ownerId} = $userId)
            LIMIT 1
        """.query[Int].run().nonEmpty
      }
    }.mapError(handleDbError)

object StorageMembersRepo:
  val layer: RLayer[Transactor, StorageMembersRepo] =
    ZLayer.fromFunction(StorageMembersRepoLive(_))
