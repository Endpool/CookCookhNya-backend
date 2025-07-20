package api.moderation

import api.moderation.pubrequests.publicationRequestEndpoints

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

val moderationEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .tag("Moderation")
    .in("moderation")

val moderationEndpoints = publicationRequestEndpoints :+ fullHistory
