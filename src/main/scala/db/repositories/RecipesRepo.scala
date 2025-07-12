package db.repositories

import db.tables.DbRecipe
import db.DbError
import domain.{IngredientId, Recipe, RecipeId}

import io.getquill.*
import java.util.UUID
import javax.sql.DataSource
import zio.{IO, ZIO, RLayer, ZLayer}
import api.Authentication.AuthenticatedUser

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: Option[String], ingredients: List[IngredientId]):
    ZIO[AuthenticatedUser, DbError, RecipeId]
  def getRecipe(recipeId: RecipeId): IO[DbError, Option[Recipe]]
  def getAll: IO[DbError, List[DbRecipe]]
  def deleteRecipe(recipeId: RecipeId): IO[DbError, Unit]

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
          .insertValue(lift(DbRecipe(id = null, name, creatorId, sourceLink)))
          .returningGenerated(r => r.id) // null is safe here because of returningGenerated
      )
      _ <- run(
        RecipeIngredientsQueries.addIngredientsQ(lift(recipeId), liftQuery(ingredientIds))
      )
    yield recipeId
  }.provideDS

  override def getRecipe(recipeId: RecipeId): IO[DbError, Option[Recipe]] = transaction {
    for
      mRecipe <- run(getRecipeQ(lift(recipeId))).map(_.headOption)
      mRecipeWithIngredients <- ZIO.foreach(mRecipe) { recipe =>
        val DbRecipe(id, name, creatorId, sourceLink) = recipe
        run(
          RecipeIngredientsQueries.getAllIngredientsQ(lift(recipe.id)))
            .map(Recipe(id, name, creatorId, _, sourceLink)
        )
      }
    yield mRecipeWithIngredients
  }.provideDS

  override def getAll: IO[DbError, List[DbRecipe]] =
    run(recipes).provideDS

  override def deleteRecipe(recipeId: RecipeId): IO[DbError, Unit] =
    run(getRecipeQ(lift(recipeId)).delete).unit.provideDS

object RecipesQueries:
  inline def getRecipeQ(inline recipeId: RecipeId): EntityQuery[DbRecipe] =
    recipes.filter(_.id == recipeId)

object RecipesRepo:
  def layer: RLayer[DataSource, RecipesRepo] =
    ZLayer.fromFunction(RecipesRepoQuill.apply)
