package db.tables.publication

import db.QuillConfig.ctx.*
import domain.PublicationRequestStatus

import java.sql.{PreparedStatement, Types}

enum DbPublicationRequestStatus:
  case Pending
  case Accepted
  case Rejected
  def toDomain(reason: Option[String]): PublicationRequestStatus = this match
    case Pending  => PublicationRequestStatus.Pending
    case Accepted => PublicationRequestStatus.Accepted
    case Rejected => PublicationRequestStatus.Rejected(reason.get) // <- unsafe code here

object DbPublicationRequestStatus:
  val fromDomain: PublicationRequestStatus => (DbPublicationRequestStatus, Option[String]) =
    case PublicationRequestStatus.Pending          => (Pending,  None)
    case PublicationRequestStatus.Accepted         => (Accepted, None)
    case PublicationRequestStatus.Rejected(reason) => (Rejected, Some(reason))

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

  given JdbcEncoder[DbPublicationRequestStatus] = encoder(
    Types.OTHER,
    (index, value, row) =>
      val statusString = value match
        case DbPublicationRequestStatus.Pending  => "pending"
        case DbPublicationRequestStatus.Accepted => "accepted"
        case DbPublicationRequestStatus.Rejected => "rejected"
      row.setString(index, statusString)
  )
