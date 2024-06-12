package delivery.producers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatten, toFunctorOps}
import common.model.OrderID
import delivery.config.KafkaConfig
import fs2.kafka._
import org.typelevel.log4cats.Logger

trait OrderFromDeliveryFailedProducer[F[_]] {
  def produce(orderId: OrderID, reason: String): EitherT[F, String, Unit]
}

object OrderFromDeliveryFailedProducer {

  def impl[F[_]: Async: Logger](config: KafkaConfig): Resource[F, OrderFromDeliveryFailedProducer[F]] = {
    val settings = ProducerSettings(Serializer[F, OrderID], Serializer[F, String]).withBootstrapServers(config.url)
    for {
      producer <- KafkaProducer.resource(settings)
    } yield new Impl[F](producer, config)
  }

  final private class Impl[F[_]: Async: Logger](
      producer: KafkaProducer[F, OrderID, String],
      config: KafkaConfig
  ) extends OrderFromDeliveryFailedProducer[F] {

    private val Topic = config.topics.orderFromDeliveryFailed

    def produce(orderId: OrderID, reason: String): EitherT[F, String, Unit] =
      producer
        .produce(ProducerRecords.one(ProducerRecord(Topic, orderId, reason)))
        .flatten
        .attemptT
        .void
        .leftMap(_.getMessage)
  }
}
