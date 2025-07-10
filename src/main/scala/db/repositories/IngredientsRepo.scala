package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbIngredient, DbIngredientCreator, ingredientsTable}
import domain.{IngredientId, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, ZIO, ZLayer}

trait IngredientsRepo:
  def addGlobal(name: String): IO[DbError, DbIngredient]
  def addPersonal(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient]
  def getGlobal(id: IngredientId): IO[DbError, Option[DbIngredient]]
  def getPersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]]
  def removeGlobal(id: IngredientId): IO[DbError, Unit]
  def removePersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def getAllGlobal: IO[DbError, Vector[DbIngredient]]
  def getAllPersonal: ZIO[AuthenticatedUser, DbError, Vector[DbIngredient]]

private final case class IngredientsRepoLive(xa: Transactor)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:
  override def addGlobal(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(None, name)))
      .mapError(handleDbError)

  override def addPersonal(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient] =
    ZIO.serviceWithZIO[AuthenticatedUser] { case AuthenticatedUser(userId) =>
      xa.transact(insertReturning(DbIngredientCreator(Some(userId), name)))
        .mapError(handleDbError)
    }

  override def getGlobal(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    xa.transact {
      findById(id).flatMap { (ingredient: DbIngredient) =>
        ingredient.ownerId match
          case Some(_) => None
          case None    => Some(ingredient)
      }
    }.mapError(handleDbError)

  override def getPersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      result  <- xa.transact {
        sql"""
           SELECT * FROM $ingredientsTable
           WHERE ${ingredientsTable.id} = $id
           AND ${ingredientsTable.ownerId} = $ownerId
           """.query[DbIngredient].run().headOption
      }.mapError(handleDbError)
    yield result

  override def removeGlobal(id: IngredientId): IO[DbError, Unit] =
    xa.transact {
      sql"""
         DELETE FROM $ingredientsTable
         WHERE ${ingredientsTable.id} = $id AND ${ingredientsTable.ownerId} IS NULL
       """.update.run()
      ()
    }.mapError(handleDbError)

  override def removePersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      _       <- xa.transact {
           sql"""
             DELETE FROM $ingredientsTable
             WHERE ${ingredientsTable.id} = $id
             AND ${ingredientsTable.ownerId} = $ownerId
           """.update.run()
      }.mapError(handleDbError)
    yield ()

  override def getAllGlobal: IO[DbError, Vector[DbIngredient]] =
    xa.transact {
      findAll.filter((ingredient: DbIngredient) => ingredient.ownerId.isEmpty)
    }.mapError(handleDbError)

  override def getAllPersonal: ZIO[AuthenticatedUser, DbError, Vector[DbIngredient]] =
    for
      ownerId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      result  <- xa.transact {
        findAll.filter { (ingredient: DbIngredient) =>
           ingredient.ownerId.contains(ownerId)
        }
      }.mapError(handleDbError)
    yield result

object IngredientsRepo:
  val layer: RLayer[Transactor, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive(_))
