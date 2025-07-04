package db.tables

import db.CustomSqlNameMapper
import domain.IngredientId

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbIngredient(
  @Id id: IngredientId,
  name: String
) derives DbCodec

final case class DbIngredientCreator(name: String)

val ingredientsTable = TableInfo[DbIngredientCreator, DbIngredient, IngredientId]
