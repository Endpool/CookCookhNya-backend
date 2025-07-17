package api.moderation.pubrequests

import api.moderation.moderationEndpoint
import api.TapirExtensions.subTag

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

val publicationRequestEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  moderationEndpoint
    .subTag("Publication Requests")
    .in("publication-requests")

val publicationRequestEndpoints = List(
  getSomePending.widen,
  getRequest.widen,
  updatePublicationRequest.widen
)
