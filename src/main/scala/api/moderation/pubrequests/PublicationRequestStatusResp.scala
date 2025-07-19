package api.moderation.pubrequests

import domain.PublicationRequestStatus

enum PublicationRequestStatusResp:
  case Pending
  case Accepted
  case Rejected(reason: Option[String])

object PublicationRequestStatusResp:
  def fromDomain(domainModel: PublicationRequestStatus): PublicationRequestStatusResp =
    domainModel match
      case PublicationRequestStatus.Accepted         => Accepted
      case PublicationRequestStatus.Pending          => Pending
      case PublicationRequestStatus.Rejected(reason) => Rejected(reason)
