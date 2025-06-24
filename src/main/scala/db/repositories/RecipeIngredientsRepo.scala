package db.repositories

import db.tables.{RecipeIngredients, Recipes}
import domain.{DbError, StorageError, IngredientId, RecipeId, StorageId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, ZIO, ZLayer}

trait RecipeIngredientsRepo:
  def getAllIngredients(recipeId: RecipeId): IO[Err, Vector[IngredientId]]
  def addIngredients(recipeId: RecipeId, ingredientIds: Vector[IngredientId]): IO[Err, Unit]
  def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit]
 
private final case class RecipeIngredientsRepoLive(xa: Transactor)
  extends Repo[RecipeIngredients, RecipeIngredients, (RecipeId, IngredientId)] with RecipeIngredientsRepo:

  override def getAllIngredients(recipeId: RecipeId): IO[Err, Vector[IngredientId]] =
    xa.transact{
      sql"""
        SELECT ${RecipeIngredients.table.ingredientId} FROM ${RecipeIngredients.table}
        WHERE ${RecipeIngredients.table.recipeId} = $recipeId
      """.query[IngredientId].run()
    }.catchAllAsDbError

  override def addIngredients(recipeId: RecipeId, ingredientIds: Vector[IngredientId]): IO[Err, Unit] =
    xa.transact {
      insertAll(ingredientIds.map(RecipeIngredients(recipeId, _)))
    }.catchAllAsDbError

  override def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit] =
    xa.transact {
      delete(RecipeIngredients(recipeId, ingredientId))
    }.catchAllAsDbError

    
object RecipeIngredientsRepo:
  val layer = ZLayer.fromFunction(RecipeIngredientsRepoLive(_))