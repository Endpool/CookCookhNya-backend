package api

import domain.PublicationRequestStatus
import io.circe.{Encoder, Json}

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

  given Encoder[PublicationRequestStatusResp] = Encoder.instance {
    case Pending => Json.fromString("pending")
    case Accepted => Json.fromString("accepted")
    case Rejected(reason) =>
     reason match
       case Some(r) => Json.fromString(s"rejected: $r")
       case None => Json.fromString("rejected")
  }