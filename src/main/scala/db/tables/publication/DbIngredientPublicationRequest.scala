package db.tables.publication

import domain.{IngredientId, IngredientPublicationRequest}

import java.time.OffsetDateTime
import java.util.UUID

final case class DbIngredientPublicationRequest(
 id: UUID,
 ingredientId: IngredientId,
 createdAt: OffsetDateTime,
 updatedAt: OffsetDateTime,
 status: DbPublicationRequestStatus,
 reason: Option[String],
):
  def toDomain: IngredientPublicationRequest =
    IngredientPublicationRequest(id, ingredientId, createdAt, updatedAt, status.toDomain(reason))

object DbIngredientPublicationRequest:
  def fromDomain(req: IngredientPublicationRequest): DbIngredientPublicationRequest =
    val (reason, status) = DbPublicationRequestStatus.fromDomain(req.status)
    DbIngredientPublicationRequest(req.id, req.ingredientId, req.createdAt, req.updatedAt, status, reason)

  val createTable: String = """
    CREATE TABLE IF NOT EXISTS ingredient_publication_requests(
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      ingredient_id UUID NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
      status publication_request_status NOT NULL DEFAULT 'pending',
      reason TEXT,
      FOREIGN KEY (ingredient_id) REFERENCES recipes(id) ON DELETE CASCADE
    );

    CREATE OR REPLACE TRIGGER update_timestamp
    BEFORE UPDATE ON ingredient_publication_requests
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
  """
