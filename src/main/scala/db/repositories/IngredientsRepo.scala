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
  def addPublic(name: String): IO[DbError, DbIngredient]
  def addCustom(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient]
  def get(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]]
  def getPublic(id: IngredientId): IO[DbError, Option[DbIngredient]]
  def getAllPublic: IO[DbError, List[DbIngredient]]
  def getAllCustom: ZIO[AuthenticatedUser, DbError, List[DbIngredient]]
  def getAll: ZIO[AuthenticatedUser, DbError, List[DbIngredient]]
  def isVisible(id: IngredientId): ZIO[AuthenticatedUser, DbError, Boolean]
  def remove(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit]

private final case class IngredientsRepoLive(xa: Transactor, dataSource: DataSource)
  extends Repo[DbIngredientCreator, DbIngredient, IngredientId] with IngredientsRepo:

  override def addPublic(name: String): IO[DbError, DbIngredient] =
    xa.transact(insertReturning(DbIngredientCreator(None, name)))
      .mapError(handleDbError)

  override def addCustom(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient] =
    ZIO.serviceWithZIO[AuthenticatedUser] { case AuthenticatedUser(userId) =>
      xa.transact(insertReturning(DbIngredientCreator(Some(userId), name)))
        .mapError(handleDbError)
    }

  override def remove(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] = for
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

  override def getAllPublic: IO[DbError, List[DbIngredient]] =
    run(getAllPublicQ).provideDS

  override def getAllCustom: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllCustomQ(lift(user.userId))).provideDS
    )

  override def getAll: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllVisibleQ(lift(user.userId))).provideDS
    )

  override def get(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getAllVisibleQ(lift(user.userId)).filter(_.id == (lift(id))))
        .map(_.headOption).provideDS
    )

  override def getPublic(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    run(getAllPublicQ.filter(_.id == (lift(id))))
      .map(_.headOption).provideDS

  override def isVisible(id: IngredientId): ZIO[AuthenticatedUser, DbError, Boolean] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(
        getAllVisibleQ(lift(user.userId))
          .filter(_.id == (lift(id)))
          .nonEmpty
      ).provideDS
    )


object IngredientsQueries:
  inline def getAllPublicQ: Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId.isEmpty)

  inline def getAllCustomQ(inline userId: UserId): Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId == Some(userId))

  inline def getAllVisibleQ(inline userId: UserId): Quoted[EntityQuery[DbIngredient]] =
    query[DbIngredient].filter(_.ownerId.forall(_ == userId))

object IngredientsRepo:
  val layer: RLayer[Transactor & DataSource, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive.apply)
