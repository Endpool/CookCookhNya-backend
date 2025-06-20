package api.db.repositories

import api.db.tables.Storages
import api.db.tables.Storages.toDomain
import api.domain.Storage.CreationEntity
import api.domain.StorageError.NotFound
import api.domain.{Storage, StorageError, StorageId, StorageView, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait StorageRepoInterface:
  def createEmpty(creationReq: CreationEntity): UIO[Storage]
  def removeById(id: StorageId): IO[NotFound, Unit]
  def getStorageViewById(id: StorageId): IO[NotFound, StorageView]
  def getAllStorageViews: UIO[Vector[StorageView]]

final case class StorageRepo(xa: Transactor) extends Repo[CreationEntity, Storages, StorageId] with StorageRepoInterface:
  override def createEmpty(creationReq: CreationEntity): UIO[Storage] =
    xa.transact {
      val newStorage: Storages = insertReturning(creationReq)
      newStorage match
        case Storages(storageId, ownerId, name) =>
          Storage(storageId, name, ownerId, Nil, Nil)
    }.catchAll(_ => ZIO.succeed(null))

  override def removeById(id: StorageId): IO[NotFound, Unit] =
    xa.transact(deleteById(id)).catchAll(_ => ZIO.unit)

  override def getStorageViewById(id: StorageId): IO[NotFound, StorageView] =
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
