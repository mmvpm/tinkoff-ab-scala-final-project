package pantry.producers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatten, toFunctorOps}
import common.model.OrderID
import fs2.kafka._
import org.typelevel.log4cats.Logger
import pantry.config.KafkaConfig

trait OrderFromPantryFailedProducer[F[_]] {
  def produce(orderId: OrderID, reason: String): EitherT[F, String, Unit]
}

object OrderFromPantryFailedProducer {

  def impl[F[_]: Async: Logger](config: KafkaConfig): Resource[F, OrderFromPantryFailedProducer[F]] = {
    val settings = ProducerSettings(Serializer[F, OrderID], Serializer[F, String]).withBootstrapServers(config.url)
    for {
      producer <- KafkaProducer.resource(settings)
    } yield new Impl[F](producer, config)
  }

  final private class Impl[F[_]: Async: Logger](
      producer: KafkaProducer[F, OrderID, String],
      config: KafkaConfig
  ) extends OrderFromPantryFailedProducer[F] {

    private val Topic = config.topics.orderFromPantryFailed

    def produce(orderId: OrderID, reason: String): EitherT[F, String, Unit] =
      producer
        .produce(ProducerRecords.one(ProducerRecord(Topic, orderId, reason)))
        .flatten
        .attemptT
        .void
        .leftMap(_.getMessage)
  }
}
