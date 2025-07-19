package db.tables.publication

import db.QuillConfig.ctx.*
import domain.PublicationRequestStatus

import java.sql.Types
import org.postgresql.util.PGobject

enum DbPublicationRequestStatus:
  case Pending
  case Accepted
  case Rejected
  def toDomain(reason: Option[String]): PublicationRequestStatus = this match
    case Pending  => PublicationRequestStatus.Pending
    case Accepted => PublicationRequestStatus.Accepted
    case Rejected => PublicationRequestStatus.Rejected(reason)

  def postgresValue: String = this match
    case Pending  => "pending"
    case Accepted => "accepted"
    case Rejected => "rejected"

object DbPublicationRequestStatus:
  val fromDomain: PublicationRequestStatus => (DbPublicationRequestStatus, Option[String]) =
    case PublicationRequestStatus.Pending          => (Pending,  None)
    case PublicationRequestStatus.Accepted         => (Accepted, None)
    case PublicationRequestStatus.Rejected(reason) => (Rejected, reason)

  inline val postgresTypeName: "publication_request_status" =
    "publication_request_status"

  val createType: String = s"""
    DO $$$$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = '$postgresTypeName') THEN
      CREATE TYPE publication_request_status AS ENUM (
        'pending',
        'accepted',
        'rejected'
      );
      END IF;
    END $$$$;
  """

  given Decoder[DbPublicationRequestStatus] =
    decoder(row => index =>
      row.getObject(index).toString match
        case "pending"  => DbPublicationRequestStatus.Pending
        case "accepted" => DbPublicationRequestStatus.Accepted
        case "rejected" => DbPublicationRequestStatus.Rejected
    )

  given Encoder[DbPublicationRequestStatus] = encoder(
    Types.OTHER,
    (index, value, row) => {
      val pgObj = new PGobject()
      pgObj.setType(postgresTypeName)
      pgObj.setValue(value.postgresValue)
      row.setObject(index, pgObj, Types.OTHER)
    }
  )
