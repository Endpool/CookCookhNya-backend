package db.tables

import db.CustomSqlNameMapper
import domain.{RecipeId, UserId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
final case class DbRecipe(
  @Id id: RecipeId,
  name: String,
  creatorId: Option[UserId],
  isPublished: Boolean,
  sourceLink: Option[String],
) derives DbCodec

case class DbRecipeCreator(name: String, creatorId: Option[UserId], sourceLink: Option[String])

val recipesTable = TableInfo[DbRecipeCreator, DbRecipe, RecipeId]
