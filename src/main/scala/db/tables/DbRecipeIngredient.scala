package db.tables

import db.CustomSqlNameMapper
import domain.{IngredientId, RecipeId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
case class DbRecipeIngredient(
  recipeId: RecipeId,
  ingredientId: IngredientId
) derives DbCodec

val recipeIngredientsTable = TableInfo[DbRecipeIngredient, DbRecipeIngredient, (RecipeId, IngredientId)]

