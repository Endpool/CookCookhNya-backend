package db.repositories

import db.tables.{DbIngredient, DbIngredientCreator}
import domain.DbError
import domain.{IngredientId}
import domain.IngredientError.NotFound

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IngredientsRepo:
  def add(name: String): IO[DbError, DbIngredient]
  def getById(id: IngredientId): IO[DbError.UnexpectedDbError, Option[DbIngredient]]
  def removeById(id: IngredientId): IO[DbError, Unit]
  def getAll: IO[DbError, Vector[DbIngredient]]

private final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:
  override def add(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(name))).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def getById(id: IngredientId): IO[DbError.UnexpectedDbError, Option[DbIngredient]] =
    xa.transact(findById(id)).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def removeById(id: IngredientId): IO[DbError, Unit] =
    xa.transact(deleteById(id)).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def getAll: IO[DbError, Vector[DbIngredient]] =
    xa.transact(findAll).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

object IngredientsRepoLive:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
