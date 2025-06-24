package db.repositories

import db.tables.{RecipeIngredients, Recipes}
import domain.{DbError, StorageError, IngredientId, RecipeId, StorageId}
import com.augustnagro.magnum.magzio.*
import zio.{IO, ZIO, ZLayer}

trait RecipesDomainRepo:
  protected type RecipeSummary = (RecipeId, String, Int, Int)
  def getSuggestedIngredients(
                               size: Int,
                               offset: Int,
                               storageIds: Vector[StorageId]
                             ): ZIO[StorageIngredientsRepo, Err | StorageError.NotFound, Vector[RecipeSummary]]
  
private final case class RecipesDomainRepoLive(xa: Transactor) extends RecipesDomainRepo:
  override def getSuggestedIngredients(
                                        size: Int,
                                        offset: Int,
                                        storageIds: Vector[StorageId]
                                      ): ZIO[StorageIngredientsRepo, Err | StorageError.NotFound, Vector[RecipeSummary]] =
    val table = RecipeIngredients.table
    for
      allIngredients<- ZIO.collectAll(storageIds.map(storageId =>
        ZIO.serviceWithZIO[StorageIngredientsRepo](_.getAllIngredientsFromStorage(storageId))
      )).map(_.flatten)
      res <- xa.transact {
        val frag =
          sql"""
            WITH recipe_stats AS (
            SELECT
              ${table.recipeId},
              r.${Recipes.table.name} AS recipe_name,
              COUNT(*) AS total_ingredients,
              SUM(CASE WHEN ${table.ingredientId} = ANY(${allIngredients.toArray}) THEN 1 ELSE 0 END) AS available_ingredients
            FROM
              $table ri
            JOIN
              ${Recipes.table} r ON r.${Recipes.table.id} = ri.${table.recipeId}
            GROUP BY
              ${table.recipeId}, recipe_name
            )
            SELECT
              ${table.recipeId},
              recipe_name,
              available_ingredients,
              total_ingredients,
              (available_ingredients::float / total_ingredients) AS availability_ratio
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

object RecipesDomainRepo:
  val layer = ZLayer.fromFunction(RecipesDomainRepoLive(_))