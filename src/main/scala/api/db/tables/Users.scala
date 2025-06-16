package api.db.tables

import com.augustnagro.magnum.*
import api.domain.{User, UserId, Storage, Ingredient, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Users(
                        @Id userId: UserId,
                        username: String,
                      ) derives DbCodec
  
object Users:
  val table = TableInfo[Users, Users, UserId]
  