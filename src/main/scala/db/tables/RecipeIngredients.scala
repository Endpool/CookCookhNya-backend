package db.tables

import com.augustnagro.magnum.*
import domain.{IngredientId, RecipeId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class RecipeIngredients(
                               recipeId: RecipeId, 
                               ingredientId: IngredientId
                             ) derives DbCodec
object RecipeIngredients:
  val table = TableInfo[RecipeIngredients, RecipeIngredients, RecipeId & IngredientId]
