package api

import domain.PublicationRequestStatus
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

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
    case Pending => Json.obj("type" -> Json.fromString("pending"))
    case Accepted => Json.obj("type" -> Json.fromString("accepted"))
    case Rejected(reason) =>
      val baseObj = Json.obj("type" -> Json.fromString("rejected"))
      reason match
        case Some(r) => baseObj.deepMerge(Json.obj("reason" -> Json.fromString(r)))
        case None => baseObj
  }

  given Decoder[PublicationRequestStatusResp] = (c: HCursor) =>
    c.downField("type").as[String].flatMap {
      case "pending" => Right(Pending)
      case "accepted" => Right(Accepted)
      case "rejected" =>
        c.downField("reason").as[Option[String]].map(Rejected.apply)
      case other => Left(DecodingFailure(s"Unknown status type: $other", c.history))
    }