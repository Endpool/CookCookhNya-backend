package api.db.repositories

import api.db.tables.StorageMembers
import api.domain.*
import com.augustnagro.magnum.magzio.*
import zio.{ZIO, IO, RLayer, ZLayer}

trait StorageMembersRepoInterface:
  def addMemberToStorageById(storageId: StorageId, memberId: UserId): IO[StorageError.NotFound | UserError.NotFound, Unit]
  def removeMemberFromStorageById(storageId: StorageId, memberId: UserId): IO[StorageError.NotFound | UserError.NotFound, Unit]
  def getAllStorageMembers(storageId: StorageId): IO[StorageError.NotFound, Vector[UserId]]

final case class StorageMembersRepo(xa: Transactor) extends Repo[StorageMembers, StorageMembers, Null]
  with StorageMembersRepoInterface:
  override def addMemberToStorageById(storageId: StorageId, memberId: UserId):
  IO[StorageError.NotFound | UserError.NotFound, Unit] =
    xa.transact(insert(StorageMembers(storageId, memberId))).catchAll {
      _ => ZIO.fail(StorageError.NotFound(storageId))
    }

  override def removeMemberFromStorageById(storageId: StorageId, memberId: UserId):
  IO[StorageError.NotFound | UserError.NotFound, Unit] =
    xa.transact(delete(StorageMembers(storageId, memberId))).catchAll(
      _ => ZIO.fail(StorageError.NotFound(storageId))
    )

  override def getAllStorageMembers(storageId: StorageId): IO[StorageError.NotFound, Vector[UserId]] =
    xa.transact {
      val s =
        sql"""
          select ${StorageMembers.table.ownerId} from ${StorageMembers.table}
          where ${StorageMembers.table.storageId} = $storageId
           """
      s.query[UserId].run()
    }.catchAll(_ => ZIO.fail(StorageError.NotFound(storageId)))

object StorageMembersRepo:
  val layer: RLayer[Transactor, StorageMembersRepoInterface] =
    ZLayer.fromFunction(StorageMembersRepo(_))
