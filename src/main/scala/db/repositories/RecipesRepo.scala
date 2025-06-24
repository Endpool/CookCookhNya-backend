package db.repositories

import db.tables.{DbRecipeIngredient, DbRecipe, DbRecipeCreator}
import domain.{IngredientId, Recipe, RecipeError, RecipeId}

import com.augustnagro.magnum.magzio.*
import zio.{ZIO, ZLayer}

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, Err, Unit]
  def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Option[Recipe]]
  def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Unit]

final case class RecipesRepoLive(xa: Transactor)
  extends Repo[DbRecipeCreator, DbRecipe, RecipeId] with RecipesRepo:

  override def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, Err, Unit] = for
      recipeId <- xa.transact {
        val recipe = DbRecipeCreator(name, sourceLink)
        val DbRecipe(recipeId, _, _) = insertReturning(recipe)
        recipeId
      }.catchAllAsDbError
      _ <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_.addIngredients(recipeId, ingredients))
    yield ()

  override def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Option[Recipe]] =
    for
      ingredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_.getAllIngredients(recipeId))
      recipeTableRowOption <- xa.transact {
        findById(recipeId)
      }.catchAllAsDbError
      recipe = recipeTableRowOption.map{ recipeTableRow =>
        Recipe(recipeId, recipeTableRow.name, ingredients, recipeTableRow.sourceLink)
      }
    yield recipe

  override def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Unit] =
    xa.transact {
      deleteById(recipeId)
    }.catchAllAsDbError

object RecipesRepo:
  val layer = ZLayer.fromFunction(RecipesRepoLive(_))
