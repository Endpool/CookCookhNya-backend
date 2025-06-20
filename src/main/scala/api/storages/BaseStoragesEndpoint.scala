package api.storages

import domain.UserId

import sttp.tapir.ztapir.*

val myStoragesEndpoint = endpoint
  .in("my" / "storages")
  .securityIn(auth.bearer[UserId]())
