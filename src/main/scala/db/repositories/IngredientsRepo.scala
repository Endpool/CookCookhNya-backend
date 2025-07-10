package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbIngredient, DbIngredientCreator, ingredientsTable}
import domain.{IngredientId, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, ZIO, ZLayer}

trait IngredientsRepo:
  def add(name: String): IO[DbError, DbIngredient]
  def addPrivate(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient]
  def getPublicById(id: IngredientId): IO[DbError, Option[DbIngredient]]
  def getById(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]]
  def removePublicById(id: IngredientId): IO[DbError, Unit]
  def removeById(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def getAllPublic: IO[DbError, Vector[DbIngredient]]
  def getAll: ZIO[AuthenticatedUser, DbError, Vector[DbIngredient]]

private final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:
  override def add(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(None, name)))
      .mapError(handleDbError)

  override def addPrivate(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient] =
    ZIO.serviceWithZIO[AuthenticatedUser] { case AuthenticatedUser(userId) =>
      xa.transact(insertReturning(DbIngredientCreator(Some(userId), name)))
        .mapError(handleDbError)
    }

  override def getPublicById(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    xa.transact {
      findById(id).flatMap { (ingredient: DbIngredient) =>
        ingredient.ownerId match
          case Some(_) => None
          case None    => Some(ingredient)
      }
    }.mapError(handleDbError)

  override def getById(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      result  <- xa.transact {
        sql"""
           SELECT * FROM $ingredientsTable
           WHERE ${ingredientsTable.id} = $id
           AND (${ingredientsTable.ownerId} = $ownerId OR ${ingredientsTable.ownerId} IS NULL)
           """.query[DbIngredient].run().headOption
      }.mapError(handleDbError)
    yield result

  override def removePublicById(id: IngredientId): IO[DbError, Unit] =
    xa.transact {
      sql"""
         DELETE FROM $ingredientsTable
         WHERE ${ingredientsTable.id} = $id AND ${ingredientsTable.ownerId} IS NULL
       """.update.run()
      ()
    }.mapError(handleDbError)

  override def removeById(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      _       <- xa.transact {
           sql"""
             DELETE FROM $ingredientsTable
             WHERE ${ingredientsTable.id} = $id
             AND (${ingredientsTable.ownerId} = $ownerId OR ${ingredientsTable.ownerId} IS NULL)
           """.update.run()
      }.mapError(handleDbError)
    yield ()

  override def getAllPublic: IO[DbError, Vector[DbIngredient]] =
    xa.transact {
      findAll.filter((ingredient: DbIngredient) => ingredient.ownerId.isEmpty)
    }.mapError(handleDbError)

  override def getAll: ZIO[AuthenticatedUser, DbError, Vector[DbIngredient]] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      result  <- xa.transact {
        findAll.filter { (ingredient: DbIngredient) =>
           ingredient.ownerId.isEmpty || ingredient.ownerId.contains(ownerId)
        }
      }.mapError(handleDbError)
    yield result

object IngredientsRepo:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
