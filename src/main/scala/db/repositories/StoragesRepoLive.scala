package db.repositories

import db.tables.Storages
import db.tables.Storages.toDomain
import domain.StorageError.NotFound
import domain.{Storage, StorageError, StorageId, StorageView, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StoragesRepo:
  def createEmpty(name: String, ownerId: UserId): UIO[Storage]
  def removeById(id: StorageId): IO[NotFound, Unit]
  def getStorageViewById(id: StorageId): IO[NotFound, StorageView]
  val getAllStorageViews: UIO[Vector[StorageView]]

private case class StorageCreationEntity(name: String, ownerId: UserId)

final case class StoragesRepoLive(xa: Transactor) extends Repo[StorageCreationEntity, Storages, StorageId] with StoragesRepo:
  override def createEmpty(name: String, ownerId: UserId): UIO[Storage] =
    xa.transact {
      val Storages(insStorageId, insName, insOwnerId): Storages = insertReturning(StorageCreationEntity(name, ownerId))
      Storage(insStorageId, insOwnerId, insName, Vector.empty, Vector.empty)
    }.catchAll(_ => ZIO.succeed(null))

  override def getStorageViewById(id: StorageId): IO[NotFound, StorageView] =
    val transaction: IO[StorageError.NotFound, Option[Storages]] =
      xa.transact(findById(id)).catchAll {
        _ => ZIO.fail(StorageError.NotFound(id))
      }

    transaction.flatMap {
      case Some(s) => ZIO.succeed(toDomain(s))
      case None    => ZIO.fail(StorageError.NotFound(id))
    }

  override val getAllStorageViews: UIO[Vector[StorageView]] =
    xa.transact(findAll.map(toDomain)).catchAll(_ => ZIO.succeed(Vector.empty))

  override def removeById(id: StorageId): IO[NotFound, Unit] =
    xa.transact(deleteById(id)).catchAll(_ => ZIO.unit)

object StoragesRepoLive:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
