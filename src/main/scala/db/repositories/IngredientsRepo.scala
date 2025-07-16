package db.repositories

import api.Authentication.AuthenticatedUser
import db.DbError
import db.tables.DbIngredient
import domain.{IngredientId, UserId}

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
  def publish(ingredientId: IngredientId): IO[DbError, Unit]

private final case class IngredientsRepoLive(dataSource: DataSource) extends IngredientsRepo:
  private given DataSource = dataSource
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientsQueries.*

  override def addPublic(name: String): IO[DbError, DbIngredient] =
    run(
      ingredientsQ
        .insert(_.name -> lift(name))
        .returning(ingredient => ingredient)
    ).provideDS

  override def addCustom(name: String): ZIO[AuthenticatedUser, DbError, DbIngredient] =
    ZIO.serviceWithZIO[AuthenticatedUser] { owner =>
      run(
        ingredientsQ
          .insert(_.name -> lift(name), _.ownerId -> Some(lift(owner.userId)))
          .returning(ingredient => ingredient)
      ).provideDS
    }

  override def remove(id: IngredientId): ZIO[AuthenticatedUser, DbError, Unit] =
    ZIO.serviceWithZIO[AuthenticatedUser] { owner =>
      run(
        customIngredientsQ(lift(owner.userId))
          .filter(_.id == lift(id))
          .delete
      ).unit.provideDS
    }

  override def getAllPublic: IO[DbError, List[DbIngredient]] =
    run(publicIngredientsQ).provideDS

  override def getAllCustom: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(customIngredientsQ(lift(user.userId))).provideDS
    )

  override def getAll: ZIO[AuthenticatedUser, DbError, List[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(visibleIngredientsQ(lift(user.userId))).provideDS
    )

  override def get(id: IngredientId): ZIO[AuthenticatedUser, DbError, Option[DbIngredient]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(visibleIngredientsQ(lift(user.userId)).filter(_.id == (lift(id))))
        .map(_.headOption).provideDS
    )

  override def getPublic(id: IngredientId): IO[DbError, Option[DbIngredient]] =
    run(publicIngredientsQ.filter(_.id == (lift(id))))
      .map(_.headOption).provideDS

  override def isVisible(id: IngredientId): ZIO[AuthenticatedUser, DbError, Boolean] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(
        visibleIngredientsQ(lift(user.userId))
          .filter(_.id == (lift(id)))
          .nonEmpty
      ).provideDS
    )

  def publish(ingredientId: IngredientId): IO[DbError, Unit] =
    run(getIngredientsQ(lift(ingredientId)).update(_.isPublished -> true)).unit.provideDS
    
object IngredientsQueries:
  inline def ingredientsQ: EntityQuery[DbIngredient] =
    query[DbIngredient]
    
  inline def publishedIngredientsQ: EntityQuery[DbIngredient] =
    ingredientsQ.filter(_.isPublished)

  inline def publicIngredientsQ: EntityQuery[DbIngredient] =
    ingredientsQ.filter(_.ownerId.isEmpty)

  inline def customIngredientsQ(inline userId: UserId): EntityQuery[DbIngredient] =
    ingredientsQ.filter(_.ownerId == Some(userId))

  inline def visibleIngredientsQ(inline userId: UserId): EntityQuery[DbIngredient] =
    ingredientsQ.filter(i => i.ownerId == None || i.ownerId == Some(userId))
    
  inline def getIngredientsQ(inline ingredientId: IngredientId): EntityQuery[DbIngredient] =
    ingredientsQ.filter(_.id == ingredientId)

object IngredientsRepo:
  val layer: RLayer[DataSource, IngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepoLive.apply)
