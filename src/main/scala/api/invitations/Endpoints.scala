package api.invitations

import sttp.tapir.ztapir.*

val invitationEndpoint =
  endpoint
    .in("invitations")

val invitationEndpoints: List[ZServerEndpoint[CreateEnv & ActivateEnv, Any]] = List(
  create.widen,
  activate.widen,
)
