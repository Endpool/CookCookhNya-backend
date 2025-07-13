package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbIngredient, DbIngredientCreator, ingredientsTable}
import domain.{IngredientId, UserId}

import com.augustnagro.magnum.magzio.*
import io.getquill.*
import javax.sql.DataSource
import zio.{IO, RLayer, ZIO, ZLayer}

trait IngredientsRepo:
  def addGlobal(name: String): IO[DbError, DbIngredient]
  def addPersonal(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient]
  def getGlobal(id: IngredientId): IO[DbError, Option[DbIngredient]]
  def getPersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]]
  def get(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]]
  def removeGlobal(id: IngredientId): IO[DbError, Unit]
  def removePersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]
  def getAllGlobal: IO[DbError, List[DbIngredient]]
  def getAllPersonal: ZIO[AuthenticatedUser, DbError, List[DbIngredient]]
  def getAllVisible: ZIO[AuthenticatedUser, DbError, List[DbIngredient]]

private final case class IngredientsRepoLive(xa: Transactor, dataSource: DataSource)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:

  override def addGlobal(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(None, name)))
      .mapError(handleDbError)

  override def addPersonal(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient] =
    ZIO.serviceWithZIO[AuthenticatedUser] { case AuthenticatedUser(userId) =>
      xa.transact(insertReturning(DbIngredientCreator(Some(userId), name)))
        .mapError(handleDbError)
    }

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

  private given DataSource = dataSource
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientsQueries.*

  override def getAllGlobal: IO[DbError, List[DbIngredient]] =
    run(getAllGlobalQ).provideDS

  override def getAllPersonal: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllPersonalQ(lift(user.userId))).provideDS
    )

  override def getAllVisible: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllVisibleQ(lift(user.userId))).provideDS
    )

  override def getGlobal(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    run(getAllGlobalQ.filter(_.id == (lift(id))))
      .map(_.headOption).provideDS

  override def getPersonal(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllPersonalQ(lift(user.userId)).filter(_.id == (lift(id))))
        .map(_.headOption).provideDS
    )

  override def get(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllVisibleQ(lift(user.userId)).filter(_.id == (lift(id))))
        .map(_.headOption).provideDS
    )

object IngredientsQueries:
  inline def getAllGlobalQ: Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId.isEmpty)

  inline def getAllPersonalQ(inline userId: UserId): Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId == Some(userId))

  inline def getAllVisibleQ(inline userId: UserId): Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId.forall(_ == userId))

object IngredientsRepo:
  val layer: RLayer[Transactor & DataSource, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive.apply)
