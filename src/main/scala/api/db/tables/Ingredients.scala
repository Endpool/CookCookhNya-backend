package api.db.tables

import com.augustnagro.magnum.*
import api.domain.{User, UserId, Storage, Ingredient, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Ingredients(
                              @Id ingredientId: IngredientId,
                              name: String
                            ) derives DbCodec:
  val toDomain: Ingredient = Ingredient(ingredientId, name)

object Ingredients:
  val table = TableInfo[Ingredient, Ingredients, IngredientId]

  def fromDomain(ingredient: Ingredient): Ingredients =
    Ingredients(ingredient.ingredientId, ingredient.name)
