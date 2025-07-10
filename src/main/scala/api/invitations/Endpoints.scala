package api.invitations

import sttp.tapir.ztapir.*

val invitationEndpoint =
  endpoint
    .tag("Invitations")
    .in("invitations")

val invitationEndpoints: List[ZServerEndpoint[CreateEnv & ActivateEnv, Any]] = List(
  create.widen,
  activate.widen,
)
