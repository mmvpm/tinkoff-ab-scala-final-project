package delivery.config

import scala.concurrent.duration.FiniteDuration

final case class KafkaConfig(
    url: String,
    groupId: String,
    batch: Int,
    timeWindow: FiniteDuration,
    topics: KafkaConfig.Topics
)

object KafkaConfig {

  final case class Topics(
      orderFromPantry: String,
      orderFromDelivery: String,
      orderFromDeliveryFailed: String
  )
}
