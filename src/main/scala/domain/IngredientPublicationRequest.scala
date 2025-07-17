package domain

import java.time.OffsetDateTime

final case class IngredientPublicationRequest(
  ingredientId: IngredientId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
)

