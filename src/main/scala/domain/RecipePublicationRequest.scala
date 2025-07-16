package domain

import java.time.OffsetDateTime
import java.util.UUID

final case class RecipePublicationRequest(
  id: UUID,
  recipeId: RecipeId,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatus,
)

