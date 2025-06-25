package db.tables

import db.CustomSqlNameMapper
import domain.{UserId, Storage, Ingredient, IngredientId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbUser(
  @Id id: UserId,
  alias: Option[String],
  fullName: String
) derives DbCodec

val usersTable = TableInfo[DbUser, DbUser, UserId]

