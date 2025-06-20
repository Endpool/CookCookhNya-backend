package api.db.repositories

import api.db.tables.{StorageCreationEntity, Storages}
import api.db.tables.Storages.toDomain
import api.domain.StorageError.NotFound
import api.domain.{Storage, StorageError, StorageId, StorageView, UserError, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StorageRepoInterface:
  def createEmpty(name: String, ownerId: UserId): UIO[Storage]
  def removeById(id: StorageId): IO[StorageError.NotFound, Unit]
  def getStorageViewById(id: StorageId): IO[StorageError.NotFound, StorageView]
  def getAllStorageViews: UIO[Vector[StorageView]]

final case class StorageRepo(xa: Transactor) extends Repo[StorageCreationEntity, Storages, StorageId] with StorageRepoInterface:
  override def createEmpty(name: String, ownerId: UserId): UIO[Storage] =
    xa.transact {
      val newStorage: Storages = insertReturning(StorageCreationEntity(name, ownerId))
      Storage(newStorage.storageId, name, ownerId, Nil, Nil)
    }.catchAll(_ => ZIO.succeed(null))

  override def removeById(id: StorageId): IO[StorageError.NotFound, Unit] =
    xa.transact(deleteById(id)).catchAll(_ => ZIO.unit)

  override def getStorageViewById(id: StorageId): IO[StorageError.NotFound, StorageView] =
    val transaction: IO[StorageError.NotFound, Option[Storages]] =
      xa.transact[Option[Storages]](findById(id)).catchAll {
        _ => ZIO.fail(StorageError.NotFound(id))
      }

    transaction.flatMap {
      case Some(s) => ZIO.succeed(toDomain(s))
      case None    => ZIO.fail(StorageError.NotFound(id))
    }

  override def getAllStorageViews: UIO[Vector[StorageView]] =
    xa.transact(findAll.map(toDomain)).catchAll(_ => ZIO.succeed(Vector.empty))

object StorageRepo:
  val layer: RLayer[Transactor, StorageRepoInterface] =
    ZLayer.fromFunction(StorageRepo(_))
