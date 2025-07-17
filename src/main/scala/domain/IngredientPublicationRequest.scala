package domain

import java.time.OffsetDateTime

final case class IngredientPublicationRequest(
  id: PublicationRequestId,
  ingredientId: IngredientId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
  comment: String
)

