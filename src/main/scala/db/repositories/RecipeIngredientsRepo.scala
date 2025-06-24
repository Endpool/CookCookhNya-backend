package db.repositories

import db.tables.{RecipeIngredients, DbRecipe, recipesTable}
import domain.{DbError, StorageError, IngredientId, RecipeId, StorageId}

import com.augustnagro.magnum.magzio.*
import zio.{IO, ZIO, ZLayer}

trait RecipeIngredientsRepo:
  protected type RecipeSummary = (RecipeId, String, Int, Int, Int)
  def getAllIngredients(recipeId: RecipeId): IO[Err, Vector[IngredientId]]
  def addIngredients(recipeId: RecipeId, ingredientIds: Vector[IngredientId]): IO[Err, Unit]
  def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[Err, Unit]
  def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, Err | StorageError.NotFound, Vector[RecipeSummary]]

final case class RecipeIngredientsRepoLive(xa: Transactor)
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

  override def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, Err | StorageError.NotFound, Vector[RecipeSummary]] =
    val table = RecipeIngredients.table
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
                SUM(CASE WHEN ${table.ingredientId} IN (${allIngredients.mkString(",")}) THEN 1 ELSE 0 END) AS available_ingredients
              FROM
                $table ri
              JOIN
                ${recipesTable} r ON r.${recipesTable.id} = ri.${table.recipeId}
              GROUP BY
                ${table.recipeId}
            )
            SELECT
              ${table.recipeId},
              recipe_name,
              available_ingredients,
              total_ingredients,
              (available_ingredients::float / total_ingredients) AS availability_ratio
              COUNT(*) AS total_recipes
            FROM
              recipe_stats
            WHERE
              total_ingredients > 0  -- Avoid division by zero
            ORDER BY
              availability_ratio DESC,
              total_ingredients DESC
            LIMIT $size
            OFFSET $offset;
          """

        frag.query[RecipeSummary].run()
      }.catchAllAsDbError
    yield res

object RecipeIngredientsRepo:
  val layer = ZLayer.fromFunction(RecipeIngredientsRepoLive(_))
