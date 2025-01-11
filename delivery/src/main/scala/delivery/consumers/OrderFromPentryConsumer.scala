package delivery.consumers

import cats.effect.{Async, Resource}
import cats.implicits._
import common.model._
import delivery.config.KafkaConfig
import delivery.producers.OrderFromDeliveryFailedProducer
import delivery.repository.DeliveryRepository
import fs2.kafka._
import io.circe.jawn
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator

final case class OrderFromPantryConsumer[F[_]: Async: Logger](
    config: KafkaConfig,
    consumer: KafkaConsumer[F, OrderID, Order],
    deliveryRepository: DeliveryRepository[F],
    orderFromDeliveryFailedProducer: OrderFromDeliveryFailedProducer[F]
) {

  def run(config: KafkaConfig): F[Unit] =
    consumer.subscribeTo(config.topics.orderFromPantry) >> consumer
      .stream
      .evalMap(record => process(record.record.value))
      .compile
      .drain

  private def process(order: Order): F[Unit] =
    for {
      _ <- info"Consumed order from pantry: $order"
      _ <- deliveryRepository.registerOrder(order).value.flatMap {
        case Left(error) =>
          for {
            _ <- warn"Failed to process order ${order.id}: $error"
            _ <- orderFromDeliveryFailedProducer.produce(order.id, error).value.map {
              case Left(producerError) =>
                error"Failed to sent event about failure to pantry for order ${order.id}: $producerError"
              case Right(_) =>
                info"Sent event about failure to pantry for order ${order.id}"
            }
          } yield ()
        case Right(_) => info"Order ${order.id} was processed"
      }
    } yield ()
}

object OrderFromPantryConsumer {

  def impl[F[_]: Async: Logger](
      config: KafkaConfig,
      deliveryRepository: DeliveryRepository[F],
      orderFromDeliveryFailedProducer: OrderFromDeliveryFailedProducer[F]
  ): Resource[F, OrderFromPantryConsumer[F]] = {
    val settings = ConsumerSettings(
      Deserializer[F, OrderID],
      Deserializer[F, String].map(jawn.decode[Order](_).toTry.get)
    )
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withBootstrapServers(config.url)
      .withGroupId(config.groupId)

    for {
      consumer <- KafkaConsumer.resource(settings)
      impl = new OrderFromPantryConsumer(config, consumer, deliveryRepository, orderFromDeliveryFailedProducer)
    } yield impl
  }
}
