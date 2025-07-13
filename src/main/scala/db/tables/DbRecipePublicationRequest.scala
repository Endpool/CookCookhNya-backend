package db.tables

import domain.RecipePublicationRequest
import domain.RecipeId

import java.time.OffsetDateTime

final case class DbRecipePublicationRequest(
  recipeId: RecipeId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: DbPublicationRequestStatus,
  reason: Option[String],
):
  def toDomain: RecipePublicationRequest =
    RecipePublicationRequest(recipeId, createdAt, updatedAt, status.toDomain(reason))

