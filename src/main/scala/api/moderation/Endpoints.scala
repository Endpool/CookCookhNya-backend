package api.moderation

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.TapirExtensions.superTag

val moderationEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .superTag("Moderation")
    .in("moderation")

val moderationEndpoints = ???
