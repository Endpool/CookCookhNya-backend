package db.tables

import com.augustnagro.magnum.*
import domain.RecipeId

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Recipes(
                        @Id id: RecipeId,
                        name: String,
                        sourceLink: String
                        ) derives DbCodec

object Recipes:
  val table = TableInfo[Recipes, Recipes, RecipeId]