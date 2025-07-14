package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.DbRecipe
import db.DbError
import domain.{UserId, IngredientId, Recipe, RecipeId}

import io.getquill.*
import java.util.UUID
import javax.sql.DataSource
import zio.{ZIO, IO, RLayer, ZLayer}

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: Option[String], ingredients: List[IngredientId]):
    ZIO[AuthenticatedUser, DbError, RecipeId]

  def getRecipe(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Option[Recipe]]
  def getAll: ZIO[AuthenticatedUser, DbError, List[DbRecipe]]
  def getAllCustom: ZIO[AuthenticatedUser, DbError, List[DbRecipe]]
  def getAllPublic: IO[DbError, List[DbRecipe]]

  def isVisible(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Boolean]
  def isPublic(recipeId: RecipeId): IO[DbError, Boolean]

  def deleteRecipe(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Unit]

  def publish(recipeId: RecipeId): IO[DbError, Unit]

final case class RecipesRepoLive(dataSource: DataSource) extends RecipesRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipesQueries.*

  private given DataSource = dataSource

  override def addRecipe(name: String, sourceLink: Option[String], ingredientIds: List[IngredientId]):
    ZIO[AuthenticatedUser, DbError, RecipeId] = transaction {
    for
      creatorId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      recipeId <- run(
        recipesQ
          .insertValue(lift(DbRecipe(id=null, name, creatorId, isPublished=false, sourceLink)))
          .returningGenerated(r => r.id) // null is safe here because of returningGenerated
      )
      _ <- run(RecipeIngredientsQueries
        .addIngredientsQ(lift(recipeId), liftQuery(ingredientIds))
      )
    yield recipeId
  }.provideDS

  override def getRecipe(recipeId: RecipeId):
    ZIO[AuthenticatedUser, DbError, Option[Recipe]] = transaction {
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      mRecipe <- run(getVisibleRecipeQ(lift(userId), lift(recipeId))).map(_.headOption)
      mRecipeWithIngredients <- ZIO.foreach(mRecipe) { recipe =>
        val DbRecipe(id, name, creatorId, isPublished, sourceLink) = recipe
        run(
          RecipeIngredientsQueries.getAllIngredientsQ(lift(recipe.id))
        ).map(Recipe(id, name, creatorId, isPublished, _, sourceLink))
      }
    yield mRecipeWithIngredients
  }.provideDS

  override def getAll: ZIO[AuthenticatedUser, DbError, List[DbRecipe]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(visibleRecipesQ(lift(user.userId))).provideDS
    )

  override def getAllCustom: ZIO[AuthenticatedUser, DbError, List[DbRecipe]] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(customRecipesQ(lift(user.userId))).provideDS
    )

  override def getAllPublic: IO[DbError, List[DbRecipe]] =
    run(publicRecipesQ).provideDS

  override def isVisible(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Boolean] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(
        visibleRecipesQ(lift(user.userId))
          .filter(_.id == lift(recipeId))
          .nonEmpty
      ).provideDS
    )

  override def isPublic(recipeId: RecipeId): IO[DbError, Boolean] =
    run(
      publicRecipesQ
        .filter(_.id == lift(recipeId))
        .nonEmpty
    ).provideDS

  override def deleteRecipe(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Unit] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getVisibleRecipeQ(lift(user.userId), lift(recipeId)).delete)
        .unit.provideDS
    )

  override def publish(recipeId: RecipeId): IO[DbError, Unit] =
    run(getRecipeQ(lift(recipeId)).update(_.isPublished -> true))
      .unit.provideDS

object RecipesQueries:
  inline def recipesQ: EntityQuery[DbRecipe] = query[DbRecipe]

  inline def publicRecipesQ: EntityQuery[DbRecipe] =
    recipesQ.filter(_.isPublished)

  inline def visibleRecipesQ(inline userId: UserId): EntityQuery[DbRecipe] =
    recipesQ.filter(r => r.isPublished || r.creatorId == userId)

  inline def customRecipesQ(inline userId: UserId): EntityQuery[DbRecipe] =
    recipesQ.filter(r => r.creatorId == userId)

  inline def getRecipeQ(inline recipeId: RecipeId): EntityQuery[DbRecipe] =
    recipesQ.filter(r => r.id == recipeId)

  inline def getVisibleRecipeQ(inline userId: UserId, inline recipeId: RecipeId): EntityQuery[DbRecipe] =
    visibleRecipesQ(userId).filter(r => r.id == recipeId)

object RecipesRepo:
  def layer: RLayer[DataSource, RecipesRepo] =
    ZLayer.fromFunction(RecipesRepoLive.apply)
