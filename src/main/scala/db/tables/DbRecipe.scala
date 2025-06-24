package db.tables

import domain.RecipeId

import com.augustnagro.magnum.*

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class DbRecipe(
  @Id id: RecipeId,
  name: String,
  sourceLink: String
) derives DbCodec

case class DbRecipeCreator(name: String, sourceLink: String)

val recipesTable = TableInfo[DbRecipeCreator, DbRecipe, RecipeId]
