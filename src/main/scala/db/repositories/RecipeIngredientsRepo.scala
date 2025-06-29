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
  def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, DbError, Vector[RecipeSummary]]

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

  override def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, DbError, Vector[RecipeSummary]] =
    val table = recipeIngredientsTable
    for
      allIngredients <- ZIO.collectAll(storageIds.map{ storageId =>
        ZIO.serviceWithZIO[StorageIngredientsRepo](_.getAllIngredientsFromStorage(storageId))
      }).map(_.flatten)
      res <- xa.transact {
        val frag =
          sql"""
            WITH recipe_stats AS (
              SELECT
                ${table.recipeId},
                r.${recipesTable.name} AS recipe_name,
                COUNT(*) AS total_ingredients,
                SUM(
                  CASE WHEN ${table.ingredientId} = ANY(${allIngredients.toArray})
                    THEN 1
                    ELSE 0
                  END
                ) AS available_ingredients
              FROM $table ri
              JOIN ${recipesTable} r ON r.${recipesTable.id} = ri.${table.recipeId}
              GROUP BY ${table.recipeId}, recipe_name
            )
            SELECT
              ${table.recipeId},
              recipe_name,
              available_ingredients,
              total_ingredients,
              COUNT(*) OVER() AS recipes_found
            FROM recipe_stats
            WHERE total_ingredients > 0  -- Avoid division by zero
            ORDER BY
              (available_ingredients::float / total_ingredients) DESC,
              total_ingredients DESC
            LIMIT $size
            OFFSET $offset;
          """
          
        frag.query[RecipeSummary].run()
      }.mapError(handleDbError)
    yield res

object RecipeIngredientsRepo:
  val layer = ZLayer.fromFunction(RecipeIngredientsRepoLive(_))
