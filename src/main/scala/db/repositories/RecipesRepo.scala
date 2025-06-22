package db.repositories

import db.tables.Recipes
import domain.{RecipeId, IngredientId, Recipe, RecipeError}

import com.augustnagro.magnum.magzio.*
import zio.ZIO
import db.tables.RecipeIngredients

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, Err, Unit]
  def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Option[Recipe]]
  def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Unit]

private final case class RecipeCreationEntity(name: String, sourceLink: String)

final case class RecipesRepoLive(xa: Transactor) extends Repo[RecipeCreationEntity, Recipes, RecipeId] with RecipesRepo:
  override def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, Err, Unit] = for
      recipeId <- xa.transact {
        val recipe = RecipeCreationEntity(name, sourceLink)
        val Recipes(recipeId, _, _) = insertReturning(recipe)
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

