package db.repositories

import db.tables.RecipeIngredients
import domain.{RecipeId, IngredientId, DbError}

import com.augustnagro.magnum.magzio.*
import zio.IO

trait RecipeIngredientsRepo:
  def getAllIngredients(recipeId: RecipeId): IO[Err, Vector[IngredientId]]
  def addIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit]
  def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit]
  def deleteRecipe(recipeId: RecipeId): IO[Err, Vector[Unit]]

final case class RecipeIngredientsRepoLive(xa: Transactor)
  extends Repo[RecipeIngredients, RecipeIngredients, (RecipeId, IngredientId)] with RecipeIngredientsRepo:

  override def getAllIngredients(recipeId: RecipeId): IO[Err, Vector[IngredientId]] =
    xa.transact{
      val frag =
        sql"""
          SELECT ${RecipeIngredients.table.ingredientId} FROM ${RecipeIngredients.table}
          WHERE ${RecipeIngredients.table.recipeId} = $recipeId
          """
      frag.query[IngredientId].run()
    }.catchAllAsDbError

  override def addIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit] =
    xa.transact {
      insert(RecipeIngredients(recipeId, ingredientId))
    }.catchAllAsDbError

  override def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit] =
    xa.transact {
      delete(RecipeIngredients(recipeId, ingredientId))
    }.catchAllAsDbError

  override def deleteRecipe(recipeId: RecipeId): IO[Err, Vector[Unit]] =
    xa.transact {
      val toDeleteSpec = Spec[RecipeIngredients]
        .where(sql"${RecipeIngredients.table.recipeId} = $recipeId")
      val toDelete: Vector[RecipeIngredients] = findAll(toDeleteSpec)
      toDelete.map(td => deleteById((td.recipeId, td.ingredientId)))
    }.catchAllAsDbError
