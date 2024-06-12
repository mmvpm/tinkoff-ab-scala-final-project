package delivery.producers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatten, toFunctorOps}
import common.model.{CourierID, OrderID}
import delivery.config.KafkaConfig
import fs2.kafka._

trait OrderFromDeliveryProducer[F[_]] {
  def produce(orderId: OrderID, courierId: CourierID): EitherT[F, String, Unit]
}

object OrderFromDeliveryProducer {

  def impl[F[_]: Async](config: KafkaConfig): Resource[F, OrderFromDeliveryProducer[F]] = {
    val settings = ProducerSettings(Serializer[F, OrderID], Serializer[F, CourierID]).withBootstrapServers(config.url)
    for {
      producer <- KafkaProducer.resource(settings)
    } yield new Impl[F](producer, config)
  }

  final private class Impl[F[_]: Async](
      producer: KafkaProducer[F, OrderID, CourierID],
      config: KafkaConfig
  ) extends OrderFromDeliveryProducer[F] {

    private val Topic = config.topics.orderFromDelivery

    def produce(orderId: OrderID, courierId: CourierID): EitherT[F, String, Unit] =
      producer
        .produce(ProducerRecords.one(ProducerRecord(Topic, orderId, courierId)))
        .flatten
        .attemptT
        .leftMap(_.getMessage)
        .void
  }
}
