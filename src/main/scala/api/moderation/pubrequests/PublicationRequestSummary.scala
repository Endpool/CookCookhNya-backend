package api.moderation.pubrequests

import java.time.OffsetDateTime
import java.util.UUID

enum PublicationRequestType:
  case Ingredient extends PublicationRequestType
  case Recipe     extends PublicationRequestType

case class PublicationRequestSummary(
  id: UUID,
  requestType: PublicationRequestType,
  entityName: String,
  createdAt: OffsetDateTime
)
