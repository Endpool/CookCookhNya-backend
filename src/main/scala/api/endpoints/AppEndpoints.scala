package api.endpoints

import api.AppEnv

import sttp.tapir.ztapir.ZServerEndpoint

object AppEndpoints:
  val endpoints: List[ZServerEndpoint[AppEnv, Any]] =
    IngredientEndpoints.endpoints ++ StorageEndpoints.endpoints
