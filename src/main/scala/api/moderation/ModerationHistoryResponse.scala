package api.moderation

import api.PublicationRequestStatusResp

import java.time.OffsetDateTime

final case class ModerationHistoryResponse(
  name: String,
  requestType: String,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  status: PublicationRequestStatusResp,
  reason: Option[String]
)
