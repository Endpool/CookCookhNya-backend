package db.repositories

import db.tables.Recipes
import domain.{RecipeId, IngredientId, Recipe, RecipeError}

import com.augustnagro.magnum.magzio.*
import zio.ZIO

trait RecipesRepo:
  def addRecipe(recipe: Recipes): ZIO[RecipeIngredientsRepo, Err, Unit]
  def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Option[Recipe]]
  def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Unit]

final case class RecipesRepoLive(xa: Transactor) extends Repo[Recipes, Recipes, RecipeId] with RecipesRepo:
  override def addRecipe(recipe: Recipes): ZIO[RecipeIngredientsRepo, Err, Unit] =
    xa.transact {
      insert(recipe)
    }.catchAllAsDbError

  override def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Option[Recipe]] =
    for
      ingredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_.getAllIngredients(recipeId))
      recipeTableRowOption <- xa.transact {
       findById(recipeId)
      }.catchAllAsDbError
      recipe = recipeTableRowOption.map(recipeTableRow =>
        Recipe(recipeId, recipeTableRow.name, ingredients, recipeTableRow.sourceLink)
      )
    yield recipe

  override def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, Err, Unit] =
    ZIO.serviceWithZIO[RecipeIngredientsRepo](_.deleteRecipe(recipeId))
    xa.transact {
      deleteById(recipeId)
    }.catchAllAsDbError

