package db.repositories

import db.tables.{DbStorage, DbStorageCreator}
import domain.StorageError.NotFound
import domain.DbError.{
  FailedDbQuery,
  DbNotRespondingError,
  UnexpectedDbError
}
import domain.{StorageId, UserId}
import db.{handleDbError, handleUnfailableQuery}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}
import domain.DbError

trait StoragesRepo:
  def createEmpty(name: String, ownerId: UserId): IO[UnexpectedDbError | DbNotRespondingError, StorageId]
  def removeById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Unit]
  def getById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Option[DbStorage]]
  val getAll: IO[UnexpectedDbError | DbNotRespondingError, Vector[DbStorage]]

final case class StoragesRepoLive(xa: Transactor)
  extends Repo[DbStorageCreator, DbStorage, StorageId] with StoragesRepo:

  override def createEmpty(name: String, ownerId: UserId): IO[UnexpectedDbError | DbNotRespondingError, StorageId] =
    xa.transact {
      val storage = insertReturning(DbStorageCreator(name, ownerId))
      storage.id
    }.mapError {
      handleDbError(_) match
        case FailedDbQuery(msg) => UnexpectedDbError(msg)
        case error: (UnexpectedDbError | DbNotRespondingError) => error
    }

  override def getById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Option[DbStorage]] =
    xa.transact {
      findById(id)
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

  override val getAll: IO[UnexpectedDbError | DbNotRespondingError, Vector[DbStorage]] =
    xa.transact {
      findAll
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

  override def removeById(id: StorageId): IO[UnexpectedDbError | DbNotRespondingError, Unit] =
    xa.transact {
      deleteById(id)
    }.mapError(e => handleUnfailableQuery(handleDbError(e)))

object StoragesRepoLive:
  val layer: RLayer[Transactor, StoragesRepo] =
    ZLayer.fromFunction(StoragesRepoLive(_))
