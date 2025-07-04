package db.repositories

import db.tables.{
  DbStorage,
  DbStorageCreator,
  recipeIngredientsTable,
  recipesTable,
  storageMembersTable,
  storagesTable
}
import domain.StorageError.NotFound
import db.{DbError, handleDbError}
import domain.{RecipeId, StorageError, StorageId, UserId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait RecipesDomainRepo:
  protected type RecipeSummary = (RecipeId, String, Int, Int, Int)
  def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, DbError, Vector[RecipeSummary]]

private final case class RecipesDomainRepoLive(xa: Transactor) extends RecipesDomainRepo:
  override def getSuggestedIngredients(
    size: Int,
    offset: Int,
    storageIds: Vector[StorageId]
  ): ZIO[StorageIngredientsRepo, DbError, Vector[RecipeSummary]] =
    val table = recipeIngredientsTable
    for
      allIngredients <- ZIO.collectAll(storageIds.map { storageId =>
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

object RecipesDomainRepo:
  val layer = ZLayer.fromFunction(RecipesDomainRepoLive(_))
