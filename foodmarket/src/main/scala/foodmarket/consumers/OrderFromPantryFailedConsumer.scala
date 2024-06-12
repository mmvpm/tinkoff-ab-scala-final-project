package foodmarket.consumers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits._
import common.model._
import foodmarket.config.KafkaConfig
import foodmarket.repository.FoodmarketRepository
import fs2.kafka._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator

final case class OrderFromPantryFailedConsumer[F[_]: Async: Logger](
    config: KafkaConfig,
    consumer: KafkaConsumer[F, OrderID, String],
    foodmarketRepository: FoodmarketRepository[F]
) {

  def run(config: KafkaConfig): F[Unit] =
    consumer.subscribeTo(config.topics.orderFromPantryFailed) >> consumer
      .stream
      .evalMap(record => process(record.record.key, record.record.value))
      .compile
      .drain

  private def process(orderId: OrderID, reason: String): F[Unit] =
    (for {
      _ <- EitherT.right[String](info"Consumed failure from pantry for order $orderId: $reason")
      _ <- foodmarketRepository.setOrderStatus(orderId, OrderStatus.Failed)
      _ <- foodmarketRepository.createFailedOrder(orderId, reason)
    } yield ())
      .value.flatMap {
        case Left(error) => error"Failed to process failure from pantry for $orderId: $error"
        case Right(_)    => ().pure[F]
      }
}

object OrderFromPantryFailedConsumer {

  def impl[F[_]: Async: Logger](
      config: KafkaConfig,
      foodmarketRepository: FoodmarketRepository[F]
  ): Resource[F, OrderFromPantryFailedConsumer[F]] = {
    val settings = ConsumerSettings(Deserializer[F, OrderID], Deserializer[F, String])
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withBootstrapServers(config.url)
      .withGroupId(config.groupId)

    for {
      consumer <- KafkaConsumer.resource(settings)
      impl = new OrderFromPantryFailedConsumer(config, consumer, foodmarketRepository)
    } yield impl
  }
}
