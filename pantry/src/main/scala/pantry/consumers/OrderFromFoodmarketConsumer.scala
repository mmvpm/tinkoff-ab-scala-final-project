package pantry.consumers

import cats.effect.{Async, Resource}
import cats.implicits._
import common.model._
import fs2.kafka._
import io.circe.jawn
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import pantry.config.KafkaConfig
import pantry.producers.OrderFromPantryFailedProducer
import pantry.repository.PantryRepository

final case class OrderFromFoodmarketConsumer[F[_]: Async: Logger](
    config: KafkaConfig,
    consumer: KafkaConsumer[F, OrderID, Order],
    pantryRepository: PantryRepository[F],
    orderFromPantryFailedProducer: OrderFromPantryFailedProducer[F]
) {

  def run(config: KafkaConfig): F[Unit] =
    consumer.subscribeTo(config.topics.orderFromFoodmarket) >> consumer
      .stream
      .evalMap(record => process(record.record.value))
      .compile
      .drain

  private def process(order: Order): F[Unit] =
    for {
      _ <- info"Consumed order from foodmarket: $order"
      _ <- pantryRepository.registerOrder(order).value.flatMap {
        case Left(error) =>
          for {
            _ <- warn"Failed to process order ${order.id}: $error"
            _ <- orderFromPantryFailedProducer.produce(order.id, error).value.map {
              case Left(producerError) =>
                error"Failed to sent event about failure to foodmarket for order ${order.id}: $producerError"
              case Right(_) =>
                info"Sent event about failure to foodmarket for order ${order.id}"
            }
          } yield ()
        case Right(_) => info"Order ${order.id} was processed"
      }
    } yield ()
}

object OrderFromFoodmarketConsumer {

  def impl[F[_]: Async: Logger](
      config: KafkaConfig,
      pantryRepository: PantryRepository[F],
      orderFromPantryFailedProducer: OrderFromPantryFailedProducer[F]
  ): Resource[F, OrderFromFoodmarketConsumer[F]] = {
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
      impl = new OrderFromFoodmarketConsumer(config, consumer, pantryRepository, orderFromPantryFailedProducer)
    } yield impl
  }
}
