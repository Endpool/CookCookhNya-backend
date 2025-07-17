package domain

import java.time.OffsetDateTime

final case class RecipePublicationRequest(
  id: PublicationRequestId,
  recipeId: RecipeId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
  comment: String
)

