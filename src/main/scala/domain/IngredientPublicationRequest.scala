package domain

import java.time.OffsetDateTime
import java.util.UUID

final case class IngredientPublicationRequest(
  id: UUID,                                           
  ingredientId: IngredientId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
  comment: String
)

