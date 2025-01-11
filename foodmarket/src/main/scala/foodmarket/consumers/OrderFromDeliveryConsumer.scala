package foodmarket.consumers

import cats.effect.{Async, Resource}
import cats.implicits._
import common.model._
import foodmarket.config.KafkaConfig
import foodmarket.repository.FoodmarketRepository
import fs2.kafka._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator

final case class OrderFromDeliveryConsumer[F[_]: Async: Logger](
    config: KafkaConfig,
    consumer: KafkaConsumer[F, OrderID, CourierID],
    foodmarketRepository: FoodmarketRepository[F]
) {

  def run(config: KafkaConfig): F[Unit] =
    consumer.subscribeTo(config.topics.orderFromDelivery) >> consumer
      .stream
      .evalMap(record => process(record.record.key, record.record.value))
      .compile
      .drain

  private def process(orderId: OrderID, courierId: CourierID): F[Unit] =
    for {
      _ <- info"Consumed event from delivery: order $orderId, courier $courierId"
      _ <- foodmarketRepository.setOrderStatus(orderId, OrderStatus.Completed).value.flatMap {
        case Left(error) => error"Failed to set status for order $orderId: $error"
        case Right(_)    => info"Order $orderId is marked as completed"
      }
    } yield ()
}

object OrderFromDeliveryConsumer {

  def impl[F[_]: Async: Logger](
      config: KafkaConfig,
      foodmarketRepository: FoodmarketRepository[F]
  ): Resource[F, OrderFromDeliveryConsumer[F]] = {
    val settings = ConsumerSettings(
      Deserializer[F, OrderID],
      Deserializer[F, CourierID]
    )
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withBootstrapServers(config.url)
      .withGroupId(config.groupId)

    for {
      consumer <- KafkaConsumer.resource(settings)
      impl = new OrderFromDeliveryConsumer(config, consumer, foodmarketRepository)
    } yield impl
  }
}
