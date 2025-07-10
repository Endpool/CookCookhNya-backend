package db.repositories

import db.tables.{DbRecipe, DbRecipeCreator}
import db.{DbError, handleDbError}
import domain.{IngredientId, Recipe, RecipeId}
import com.augustnagro.magnum.magzio.*
import zio.{ZIO, IO, ZLayer}

trait RecipesRepo:
  def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, DbError, RecipeId]
  def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, DbError, Option[Recipe]]
  def getAll: IO[DbError, Vector[DbRecipe]]
  def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, DbError, Unit]

private final case class RecipesRepoLive(xa: Transactor)
  extends Repo[DbRecipeCreator, DbRecipe, RecipeId] with RecipesRepo:

  override def addRecipe(name: String, sourceLink: String, ingredients: Vector[IngredientId]):
    ZIO[RecipeIngredientsRepo, DbError, RecipeId] = for
      recipeId <- xa.transact {
        val recipe = DbRecipeCreator(name, sourceLink)
        val DbRecipe(recipeId, _, _) = insertReturning(recipe)
        recipeId
      }.mapError(handleDbError)
      _ <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_.addIngredients(recipeId, ingredients))
    yield recipeId

  override def getRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, DbError, Option[Recipe]] =
    for
      ingredients <- ZIO.serviceWithZIO[RecipeIngredientsRepo](_.getAllIngredients(recipeId))
      recipeTableRowOption <- xa.transact {
        findById(recipeId)
      }.mapError(handleDbError)
      recipe = recipeTableRowOption.map{ recipeTableRow =>
        Recipe(recipeId, recipeTableRow.name, ingredients, recipeTableRow.sourceLink)
      }
    yield recipe

  override def getAll: IO[DbError, Vector[DbRecipe]] =
    xa.transact(findAll).mapError(handleDbError)
    
  override def deleteRecipe(recipeId: RecipeId): ZIO[RecipeIngredientsRepo, DbError, Unit] =
    xa.transact {
      deleteById(recipeId)
    }.mapError(handleDbError)

object RecipesRepo:
  val layer = ZLayer.fromFunction(RecipesRepoLive(_))
