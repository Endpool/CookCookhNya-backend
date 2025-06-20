package api

import api.ingredients.ingredientsEndpoints
import api.storages.storageEndpoints

import sttp.tapir.ztapir.ZServerEndpoint

object AppEndpoints:
  val endpoints: List[ZServerEndpoint[AppEnv, Any]]
    =  storageEndpoints
    ++ ingredientsEndpoints
