package db.repositories

import db.tables.{DbIngredient, DbIngredientCreator}
import domain.DbError.{UnexpectedDbError, DbNotRespondingError}
import domain.IngredientId
import db.{handleDbError, handleUnfailableQuery}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IngredientsRepo:
  def add(name: String): IO[UnexpectedDbError | DbNotRespondingError, DbIngredient]
  def getById(id: IngredientId): IO[UnexpectedDbError | DbNotRespondingError, Option[DbIngredient]]
  def removeById(id: IngredientId): IO[UnexpectedDbError | DbNotRespondingError, Unit]
  def getAll: IO[UnexpectedDbError | DbNotRespondingError, Vector[DbIngredient]]

final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:
  override def add(name: String): IO[UnexpectedDbError | DbNotRespondingError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(name))).mapError {
      e => handleUnfailableQuery(handleDbError(e))
    }

  override def getById(id: IngredientId): IO[UnexpectedDbError | DbNotRespondingError, Option[DbIngredient]] =
    xa.transact(findById(id)).mapError {
      e => handleUnfailableQuery(handleDbError(e))
    }

  override def removeById(id: IngredientId): IO[UnexpectedDbError | DbNotRespondingError, Unit] =
    xa.transact(deleteById(id)).mapError {
      e => handleUnfailableQuery(handleDbError(e))
    }

  override def getAll: IO[UnexpectedDbError | DbNotRespondingError, Vector[DbIngredient]] =
    xa.transact(findAll).mapError {
      e => handleUnfailableQuery(handleDbError(e))
    }

object IngredientsRepoLive:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
