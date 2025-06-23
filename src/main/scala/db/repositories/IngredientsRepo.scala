package db.repositories

import db.tables.Ingredients
import domain.DbError
import domain.{IngredientId}
import domain.IngredientError.NotFound

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IngredientsRepo:
  def add(name: String): IO[DbError, Ingredients]
  def getById(id: IngredientId): IO[DbError, Option[Ingredients]]
  def removeById(id: IngredientId): IO[DbError, Unit]
  def getAll: IO[DbError, Vector[Ingredients]]

private final case class IngredientCreator(name: String)

final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[IngredientCreator, Ingredients, IngredientId] with IngredientsRepo:
  override def add(name: String): IO[DbError, Ingredients] =
    xa.transact(insertReturning(IngredientCreator(name))).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def getById(id: IngredientId): IO[DbError, Option[Ingredients]] =
    xa.transact(findById(id)).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def removeById(id: IngredientId): IO[DbError, Unit] =
    xa.transact(deleteById(id)).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

  override def getAll: IO[DbError, Vector[Ingredients]] =
    xa.transact(findAll).catchAll { e =>
      ZIO.fail(DbError.UnexpectedDbError(e.getMessage()))
    }

object IngredientsRepoLive:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
