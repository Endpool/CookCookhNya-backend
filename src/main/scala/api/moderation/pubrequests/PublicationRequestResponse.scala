package api.moderation.pubrequests

import domain.PublicationRequestStatus

import java.time.OffsetDateTime
import java.util.UUID

case class PublicationRequestResponse(
  id: UUID,
  requestType: PublicationRequestType,
  entityId: UUID,
  entityName: String,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  comment: String,
  status: PublicationRequestStatus
)
