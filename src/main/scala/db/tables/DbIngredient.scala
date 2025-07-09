package db.tables

import db.CustomSqlNameMapper
import domain.{IngredientId, UserId}
import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbIngredient(
  @Id id: IngredientId,
  ownerId: Option[UserId],
  name: String
) derives DbCodec

final case class DbIngredientCreator(ownerId: Option[UserId], name: String)

val ingredientsTable = TableInfo[DbIngredientCreator, DbIngredient, IngredientId]
