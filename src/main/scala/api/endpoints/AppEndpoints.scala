package api.endpoints

import sttp.tapir.ztapir.ZServerEndpoint

import api.AppEnv

object AppEndpoints:
  val endpoints: List[ZServerEndpoint[AppEnv, Any]] =
    IngredientEndpoints.endpoints ++ StoragesIngredientEndpoints.endpoints

