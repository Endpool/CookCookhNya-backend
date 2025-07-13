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

object DbRecipePublicationRequest:
  def fromDomain(req: RecipePublicationRequest): DbRecipePublicationRequest =
    val (reason, status) = DbPublicationRequestStatus.fromDomain(req.status)
    DbRecipePublicationRequest(req.recipeId, req.createdAt, req.updatedAt, status, reason)

  val createTable: String = """
    CREATE TABLE recipe_publication_request(
      recipe_id UUID NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      status publication_request_status NOT NULL DEFAULT 'pending',
      reason TEXT,
      FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
    );
  """
