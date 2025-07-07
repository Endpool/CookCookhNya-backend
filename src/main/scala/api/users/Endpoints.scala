package api.users

import domain.UserId
import sttp.tapir.ztapir.*

val usersEndpoint =
  endpoint
    .in("users")
    .securityIn(auth.bearer[UserId]())

val usersEndpoints = List(
  create.widen
)
