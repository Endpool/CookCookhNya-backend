package domain

import java.time.OffsetDateTime

final case class RecipePublicationRequest(
  recipeId: RecipeId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
)

