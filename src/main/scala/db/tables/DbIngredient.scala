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

object DbIngredient:
  val createTable: String = """
    CREATE TABLE IF NOT EXISTS ingredients(
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      owner_id BIGINT DEFAULT NULL,
      name VARCHAR(255) NOT NULL,
      FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
    );
  """
