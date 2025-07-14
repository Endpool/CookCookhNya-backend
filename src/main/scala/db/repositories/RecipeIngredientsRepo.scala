package db.repositories

import db.tables.DbRecipeIngredient
import db.DbError
import domain.{IngredientId, RecipeId}

import io.getquill.*
import javax.sql.DataSource
import zio.{IO, ZIO, RLayer, ZLayer}

trait RecipeIngredientsRepo:
  def getAllIngredients(recipeId: RecipeId): IO[DbError, List[IngredientId]]
  def addIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[DbError, Unit]
  def addIngredients(recipeId: RecipeId, ingredientIds: List[IngredientId]): IO[DbError, Unit]
  def removeIngredient(recipeId: RecipeId, ingredientId: IngredientId): IO[DbError, Unit]

private inline def recipeIngredients = query[DbRecipeIngredient]

final case class RecipeIngredientsRepoLive(dataSource: DataSource) extends RecipeIngredientsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipeIngredientsQueries.*

  private given DataSource = dataSource

  override def getAllIngredients(recipeId: RecipeId):
    IO[DbError, List[IngredientId]] =
    run(getAllIngredientsQ(lift(recipeId))).provideDS

  override def addIngredient(recipeId: RecipeId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    run(
      addIngredientQ(lift(recipeId), lift(ingredientId))
        .onConflictIgnore
    ).unit.provideDS

  override def addIngredients(recipeId: RecipeId, ingredientIds: List[IngredientId]):
    IO[DbError, Unit] =
    run(
      liftQuery(ingredientIds).foreach(
        addIngredientQ(lift(recipeId), _)
          .onConflictIgnore
      )
    ).unit.provideDS

  override def removeIngredient(recipeId: RecipeId, ingredientId: IngredientId):
    IO[DbError, Unit] =
    run(deleteIngredientQ(lift(recipeId), lift(ingredientId))).unit.provideDS

object RecipeIngredientsQueries:
  import db.QuillConfig.ctx.*

  inline def getAllIngredientsQ(inline recipeId: RecipeId) =
    recipeIngredients
      .filter(_.recipeId == recipeId)
      .map(_.ingredientId)

  inline def addIngredientQ(inline recipeId: RecipeId, inline ingredientIds: IngredientId) =
    recipeIngredients.insertValue((DbRecipeIngredient(recipeId, ingredientIds)))

  inline def deleteIngredientQ(inline recipeId: RecipeId, inline ingredientId: IngredientId) =
    recipeIngredients
      .filter(ri => ri.recipeId     == recipeId
                 && ri.ingredientId == ingredientId)
      .delete

object RecipeIngredientsRepoLive:
  def layer: RLayer[DataSource, RecipeIngredientsRepo] =
    ZLayer.fromFunction(RecipeIngredientsRepoLive.apply)

