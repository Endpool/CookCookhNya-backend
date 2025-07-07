package api.users

import domain.UserId
import sttp.tapir.ztapir.*

val usersEndpoint =
  endpoint
    .in("users")

val usersEndpoints = List(
  create.widen
)
