package api.invitations

import sttp.tapir.ztapir.*

val invitationEndpoint =
  endpoint
    .in("invitations")
//
//val invitationEndpoints = List(
//  create.widen,
//  get.widen,
//)
