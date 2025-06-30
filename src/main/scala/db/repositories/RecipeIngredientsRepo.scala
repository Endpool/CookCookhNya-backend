package db.repositories

import db.tables.{DbRecipeIngredient, recipeIngredientsTable, DbRecipe, recipesTable}
import db.{DbError, handleDbError}
import domain.{StorageError, RecipeError, IngredientId, RecipeId, StorageId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, ZIO, ZLayer}

trait RecipeIngredientsRepo:
  protected type RecipeSummary = (RecipeId, String, Int, Int, Int)
  def getAllIngredients(recipeId: RecipeId): IO[DbError, Vector[IngredientId]]
  def addIngredients(recipeId: RecipeId, ingredientIds: Vector[IngredientId]): IO[DbError, Unit]
  def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[DbError, Unit]

private final case class RecipeIngredientsRepoLive(xa: Transactor)
  extends Repo[DbRecipeIngredient, DbRecipeIngredient, (RecipeId, IngredientId)]
  with RecipeIngredientsRepo:

  override def getAllIngredients(recipeId: RecipeId): IO[DbError, Vector[IngredientId]] =
    xa.transact{
      sql"""
        SELECT ${recipeIngredientsTable.ingredientId} FROM ${recipeIngredientsTable}
        WHERE ${recipeIngredientsTable.recipeId} = $recipeId
      """.query[IngredientId].run()
    }.mapError(handleDbError)

  override def addIngredients(recipeId: RecipeId, ingredientIds: Vector[IngredientId]): IO[DbError, Unit] =
    xa.transact {
      insertAll(ingredientIds.map(DbRecipeIngredient(recipeId, _)))
    }.mapError(handleDbError)

  override def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[DbError, Unit] =
    xa.transact {
      delete(DbRecipeIngredient(recipeId, ingredientId))
    }.mapError(handleDbError)

object RecipeIngredientsRepo:
  val layer = ZLayer.fromFunction(RecipeIngredientsRepoLive(_))
