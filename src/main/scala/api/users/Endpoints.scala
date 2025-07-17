package api.users

import sttp.tapir.ztapir.*

val usersEndpoint =
  endpoint
    .tag("Users")
    .in("users")

val usersEndpoints = List(
  create.widen
)
