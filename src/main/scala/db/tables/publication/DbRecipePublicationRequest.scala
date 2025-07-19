package db.tables.publication

import domain.{RecipeId, RecipePublicationRequest}

import java.time.OffsetDateTime
import java.util.UUID

final case class DbRecipePublicationRequest(
  id: UUID,
  recipeId: RecipeId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: DbPublicationRequestStatus,
  reason: Option[String],
):
  def toDomain: RecipePublicationRequest =
    RecipePublicationRequest(id, recipeId, createdAt, updatedAt, status.toDomain(reason))

object DbRecipePublicationRequest:
  def fromDomain(req: RecipePublicationRequest): DbRecipePublicationRequest =
    val (status, reason) = DbPublicationRequestStatus.fromDomain(req.status)
    DbRecipePublicationRequest(req.id, req.recipeId, req.createdAt, req.updatedAt, status, reason)

  val createTable: String = s"""
    CREATE TABLE IF NOT EXISTS recipe_publication_requests(
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      recipe_id UUID NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      status ${DbPublicationRequestStatus.postgresTypeName} NOT NULL DEFAULT 'pending',
      reason TEXT,
      FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
    );

    CREATE OR REPLACE TRIGGER recipe_publication_requests_update_timestamp
    BEFORE UPDATE ON recipe_publication_requests
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
  """
