package api.moderation.pubrequests

import api.moderation.moderationEndpoint
import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

import api.TapirExtensions.subTag

val publicationRequestEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  moderationEndpoint
    .subTag("publication-requests")
    .in("publication-requests")

val publicationRequestEndpoints = List(
  getSomePending.widen,
  getRequest.widen,
  updatePublicationRequest.widen
)
