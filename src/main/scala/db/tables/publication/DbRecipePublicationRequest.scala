package db.tables.publication

import domain.{RecipeId, RecipePublicationRequest}

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
    CREATE TABLE IF NOT EXISTS recipe_publication_requests(
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      recipe_id UUID NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      status publication_request_status NOT NULL DEFAULT 'pending',
      reason TEXT,
      FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
    );

    CREATE OR REPLACE FUNCTION trigger_set_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
      RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    CREATE OR REPLACE TRIGGER update_timestamp
    BEFORE UPDATE ON recipe_publication_requests
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
  """
