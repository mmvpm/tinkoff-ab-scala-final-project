package pantry.consumers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits._
import common.model._
import fs2.kafka._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import pantry.config.KafkaConfig
import pantry.producers.OrderFromPantryFailedProducer
import pantry.repository.PantryRepository

final case class OrderFromDeliveryFailedConsumer[F[_]: Async: Logger](
    config: KafkaConfig,
    consumer: KafkaConsumer[F, OrderID, String],
    pantryRepository: PantryRepository[F],
    orderFromPantryFailedProducer: OrderFromPantryFailedProducer[F]
) {

  def run(config: KafkaConfig): F[Unit] =
    consumer.subscribeTo(config.topics.orderFromDeliveryFailed) >> consumer
      .stream
      .evalMap(record => process(record.record.key, record.record.value))
      .compile
      .drain

  private def process(orderId: OrderID, reason: String): F[Unit] =
    for {
      _ <- info"Consumed failure from delivery for order $orderId: $reason"
      _ <- rollbackOrder(orderId).value.flatMap {
        case Left(error) => error"Failed to rollback order $orderId: $error"
        case Right(_)    => ().pure[F]
      }
      _ <- orderFromPantryFailedProducer.produce(orderId, reason).value.map {
        case Left(producerError) =>
          error"Failed to sent event about failure to foodmarket for order $orderId: $producerError"
        case Right(_) =>
          info"Sent event about failure to foodmarket for order $orderId"
      }
    } yield ()

  private def rollbackOrder(orderId: OrderID): EitherT[F, String, Unit] =
    for {
      optOrder <- pantryRepository.getOrder(orderId)
      order <- EitherT.fromOption[F](optOrder, s"order $orderId is not found")
      _ <- order.products.traverse(pantryRepository.updateProductCount(_, amount = 1))
    } yield ()
}

object OrderFromDeliveryFailedConsumer {

  def impl[F[_]: Async: Logger](
      config: KafkaConfig,
      pantryRepository: PantryRepository[F],
      orderFromPantryFailedProducer: OrderFromPantryFailedProducer[F]
  ): Resource[F, OrderFromDeliveryFailedConsumer[F]] = {
    val settings = ConsumerSettings(Deserializer[F, OrderID], Deserializer[F, String])
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)
      .withBootstrapServers(config.url)
      .withGroupId(config.groupId)

    for {
      consumer <- KafkaConsumer.resource(settings)
      impl = new OrderFromDeliveryFailedConsumer(config, consumer, pantryRepository, orderFromPantryFailedProducer)
    } yield impl
  }
}
