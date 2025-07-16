package api.moderation.pubrequests

import domain.PublicationRequestStatus

case class PublicationRequestUpdate(
  comment: String,
  status: PublicationRequestStatus
)
