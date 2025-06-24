package db.repositories

import db.tables.{DbStorage, DbStorageCreator}
import domain.{StorageId, UserId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}
import domain.DbError

trait StoragesRepo:
  def createEmpty(name: String, ownerId: UserId): IO[DbError.UnexpectedDbError, StorageId]
  def removeById(id: StorageId): IO[DbError.UnexpectedDbError, Unit]
  def getById(id: StorageId): IO[DbError.UnexpectedDbError, Option[DbStorage]]
  val getAll: UIO[Vector[DbStorage]]

private final case class StoragesRepoLive(xa: Transactor)
  extends Repo[DbStorageCreator, DbStorage, StorageId] with StoragesRepo:

  override def createEmpty(name: String, ownerId: UserId): IO[DbError.UnexpectedDbError, StorageId] =
    xa.transact {
      val storage = insertReturning(DbStorageCreator(name, ownerId))
      storage.id
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

  override def getById(id: StorageId): IO[DbError.UnexpectedDbError, Option[DbStorage]] =
    xa.transact {
      findById(id)
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

  override val getAll: UIO[Vector[DbStorage]] =
    xa.transact {
      findAll
    }.catchAll(_ => ZIO.succeed(Vector.empty))

  override def removeById(id: StorageId): IO[DbError.UnexpectedDbError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage()))

object StoragesRepoLive:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
