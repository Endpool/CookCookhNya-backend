package api

import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import zio.RIO

object MetricsConfig {
  // Define the effect type matching your endpoints
  type Eff[A] = RIO[AppEnv, A]
  
  // Initialize Prometheus metrics with the correct effect type
  val metrics: PrometheusMetrics[Eff] = PrometheusMetrics.default[Eff]()
}