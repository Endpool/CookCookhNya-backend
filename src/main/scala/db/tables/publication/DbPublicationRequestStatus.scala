package db.tables.publication

import db.QuillConfig.ctx.*
import domain.PublicationRequestStatus

enum DbPublicationRequestStatus:
  case Pending
  case Accepted
  case Rejected
  def toDomain(reason: Option[String]): PublicationRequestStatus = this match
    case Pending  => PublicationRequestStatus.Pending
    case Accepted => PublicationRequestStatus.Accepted
    case Rejected => PublicationRequestStatus.Rejected(reason.get) // <- unsafe code here

object DbPublicationRequestStatus:
  val fromDomain: PublicationRequestStatus => (Option[String], DbPublicationRequestStatus) =
    case PublicationRequestStatus.Pending          => (None, Pending)
    case PublicationRequestStatus.Accepted         => (None, Accepted)
    case PublicationRequestStatus.Rejected(reason) => (Some(reason), Rejected)

  val createType: String = """
  DO $$
  BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'publication_request_status') THEN
    CREATE TYPE publication_request_status AS ENUM (
      'pending',
      'accepted',
      'rejected'
    );
    END IF;
  END $$;
  """

  given JdbcDecoder[DbPublicationRequestStatus](
    (index, row, _) =>
      val statusType = row.getString(index)
      statusType match
        case "pending"  => DbPublicationRequestStatus.Pending
        case "accepted" => DbPublicationRequestStatus.Accepted
        case "rejected" => DbPublicationRequestStatus.Rejected
  )
