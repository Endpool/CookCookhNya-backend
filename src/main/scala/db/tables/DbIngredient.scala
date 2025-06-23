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

object DbIngredient:
  val table = TableInfo[Ingredient, DbIngredient, IngredientId]
