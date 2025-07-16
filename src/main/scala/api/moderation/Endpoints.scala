package api.moderation

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*
import api.TapirExtensions.superTag
import api.moderation.pubrequests.publicationRequestEndpoints

val moderationEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .superTag("Moderation")
    .in("moderation")

val moderationEndpoints = publicationRequestEndpoints
