package db.repositories

import db.tables.{DbRecipeIngredient, recipeIngredientsTable, DbRecipe, recipesTable}
import db.{DbError, handleDbError}
import db.QuillConfig.provideDS
import domain.{IngredientId, RecipeId, StorageId}

import io.getquill.*
import javax.sql.DataSource
import zio.{IO, ZIO, RLayer, ZLayer}

trait RecipeIngredientsRepo:
  def getAllIngredients(recipeId: RecipeId): IO[DbError, List[IngredientId]]
  def addIngredients(recipeId: RecipeId, ingredientIds: List[IngredientId]): IO[DbError, Unit]
  def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[DbError, Unit]

private inline def recipeIngredients = query[DbRecipeIngredient]

final case class RecipeIngredientsRepoLive(dataSource: DataSource) extends RecipeIngredientsRepo:
  import db.QuillConfig.ctx.*
  import RecipeIngredientsQueries.*

  given DataSource = dataSource

  override def getAllIngredients(recipeId: RecipeId):
    IO[DbError, List[IngredientId]] =
    run(getAllIngredientsQ(lift(recipeId))).provideDS

  override def addIngredients(recipeId: RecipeId, ingredientIds: List[IngredientId]):
    IO[DbError, Unit] =
    run(addIngredientsQ(lift(recipeId), liftQuery(ingredientIds))).unit.provideDS

  override def deleteIngredient(recipeId: RecipeId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    run(deleteIngredientQ(lift(recipeId), lift(ingredientId))).unit.provideDS

object RecipeIngredientsQueries:
  import db.QuillConfig.ctx.*

  inline def getAllIngredientsQ(inline recipeId: RecipeId) =
    recipeIngredients
      .filter(_.recipeId == recipeId)
      .map(_.ingredientId)

  inline def addIngredientsQ(inline recipeId: RecipeId, inline ingredientIds: Query[IngredientId]) =
    ingredientIds.foreach(id => recipeIngredients.insertValue((DbRecipeIngredient(recipeId, id))))

  inline def deleteIngredientQ(inline recipeId: RecipeId, inline ingredientId: IngredientId) =
    recipeIngredients
      .filter(ri => ri.recipeId     == recipeId
                 && ri.ingredientId == ingredientId)
      .delete

object RecipeIngredientsRepoLive:
  def layer: RLayer[DataSource, RecipeIngredientsRepo] =
    ZLayer.fromFunction(RecipeIngredientsRepoLive.apply)

