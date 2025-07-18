package domain

enum PublicationRequestStatus:
  case Pending
  case Accepted
  case Rejected(reason: Option[String])

