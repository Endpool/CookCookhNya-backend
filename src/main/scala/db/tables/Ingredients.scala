package db.tables

import com.augustnagro.magnum.*
import domain.{Ingredient, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Ingredients(
  @Id id: IngredientId,
  name: String
) derives DbCodec:
  def toDomain = Ingredient(id, name)

object Ingredients:
  val table = TableInfo[Ingredient, Ingredients, IngredientId]
