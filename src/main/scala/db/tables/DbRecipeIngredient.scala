package db.tables

import com.augustnagro.magnum.*
import domain.{IngredientId, RecipeId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class DbRecipeIngredient(
  recipeId: RecipeId,
  ingredientId: IngredientId
) derives DbCodec

val recipeIngredientsTable = TableInfo[DbRecipeIngredient, DbRecipeIngredient, (RecipeId, IngredientId)]

