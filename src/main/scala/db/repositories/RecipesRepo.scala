package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.DbRecipe
import db.DbError
import domain.{UserId, IngredientId, Recipe, RecipeId}

import io.getquill.*
import java.util.UUID
import javax.sql.DataSource
import zio.{ZIO, RLayer, ZLayer}

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: Option[String], ingredients: List[IngredientId]):
    ZIO[AuthenticatedUser, DbError, RecipeId]
  def getRecipe(recipeId: RecipeId):
    ZIO[AuthenticatedUser, DbError, Option[Recipe]]
  def getAll:
    ZIO[AuthenticatedUser, DbError, List[DbRecipe]]
  def deleteRecipe(recipeId: RecipeId):
    ZIO[AuthenticatedUser, DbError, Unit]

private inline def recipes = query[DbRecipe]

final case class RecipesRepoQuill(dataSource: DataSource) extends RecipesRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipesQueries.*

  private given DataSource = dataSource

  override def addRecipe(name: String, sourceLink: Option[String], ingredientIds: List[IngredientId]):
    ZIO[AuthenticatedUser, DbError, RecipeId] = transaction {
    for
      creatorId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      recipeId <- run(
        recipes
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
      mRecipe <- run(getRecipeQ(lift(userId), lift(recipeId))).map(_.headOption)
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

  override def deleteRecipe(recipeId: RecipeId): ZIO[AuthenticatedUser, DbError, Unit] =
    ZIO.serviceWithZIO[AuthenticatedUser](user =>
      run(getRecipeQ(lift(user.userId), lift(recipeId)).delete)
        .unit.provideDS
    )

object RecipesQueries:
  inline def visibleRecipesQ(inline userId: UserId): EntityQuery[DbRecipe] =
    recipes.filter(r => r.isPublished || r.creatorId == userId)

  inline def getRecipeQ(inline userId: UserId, inline recipeId: RecipeId): EntityQuery[DbRecipe] =
    visibleRecipesQ(userId).filter(r => r.id == recipeId)

object RecipesRepo:
  def layer: RLayer[DataSource, RecipesRepo] =
    ZLayer.fromFunction(RecipesRepoQuill.apply)
