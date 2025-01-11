package delivery.metrics

import io.prometheus.metrics.model.registry.PrometheusRegistry
import sttp.tapir.server.metrics.MetricLabels
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

object ServerMetrics {

  private val namespace = "pantry"

  def register[F[_]](prometheusRegistry: PrometheusRegistry): PrometheusMetrics[F] =
    PrometheusMetrics.default[F](namespace, prometheusRegistry, MetricLabels.Default)
}
