package db.tables

import db.CustomSqlNameMapper
import domain.{Ingredient, IngredientId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbIngredient(
  @Id id: IngredientId,
  name: String
) derives DbCodec:
  def toDomain = Ingredient(id, name)

final case class DbIngredientCreator(name: String)

val ingredientsTable = TableInfo[DbIngredientCreator, DbIngredient, IngredientId]
