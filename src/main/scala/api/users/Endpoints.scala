package api.users

import sttp.tapir.ztapir.*

val usersEndpoint =
  endpoint
    .in("users")

val usersEndpoints = List(
  create
)