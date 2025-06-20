package db.tables

import com.augustnagro.magnum.*
import domain.{User, UserId, Storage, Ingredient, IngredientId}

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
final case class Users(
                        @Id id: UserId,
                        alias: Option[String],
                        fullName: String
                      ) derives DbCodec

object Users:
  val table = TableInfo[Users, Users, UserId]

