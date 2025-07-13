package api

import api.ingredients.ingredientsEndpoints
import api.storages.storageEndpoints
import api.users.usersEndpoints
import api.recipes.recipeEndpoints
import api.shoppinglist.shoppingListEndpoints
import api.invitations.invitationEndpoints

import sttp.tapir.metrics.prometheus.PrometheusMetrics 
import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.ztapir.RichZServerEndpoint
import zio.Task 

object AppEndpoints:
  // Initialize Prometheus metrics
  private val metrics = PrometheusMetrics.default[Task]()

  // Initialise API endpoints
  private val apiEndpoints: List[ZServerEndpoint[AppEnv, Any]] =
    storageEndpoints.map(_.widen) ++
    ingredientsEndpoints.map(_.widen) ++
    usersEndpoints.map(_.widen) ++
    recipeEndpoints.map(_.widen) ++
    shoppingListEndpoints.map(_.widen) ++
    invitationEndpoints.map(_.widen)

  // Expose the whole list of endpoints
  val endpoints: List[ZServerEndpoint[AppEnv, Any]] =
    apiEndpoints :+ metrics.metricsEndpoint.widen[AppEnv]
