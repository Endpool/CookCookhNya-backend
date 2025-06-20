package api.db.tables

import com.augustnagro.magnum.*
import api.domain.{Ingredient, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Ingredients(
                              @Id ingredientId: IngredientId,
                              name: String
                            ) derives DbCodec

object Ingredients:
  val table = TableInfo[Ingredient, Ingredients, IngredientId]

  def toDomain(ingredient: Ingredients): Ingredient = ingredient match
    case Ingredients(ingredientId, name) => Ingredient(ingredientId, name)
