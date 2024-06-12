package pantry.producers

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatten, toFunctorOps}
import common.model.{Order, OrderID}
import fs2.kafka._
import io.circe.syntax.EncoderOps
import pantry.config.KafkaConfig

trait OrderFromPantryProducer[F[_]] {
  def produce(order: Order): EitherT[F, String, Unit]
}

object OrderFromPantryProducer {

  def impl[F[_]: Async](config: KafkaConfig): Resource[F, OrderFromPantryProducer[F]] = {
    val serializer = Serializer[F, String].contramap[Order](_.asJson.noSpaces)
    val settings = ProducerSettings(Serializer[F, OrderID], serializer).withBootstrapServers(config.url)
    for {
      producer <- KafkaProducer.resource(settings)
    } yield new Impl[F](producer, config)
  }

  final private class Impl[F[_]: Async](
      producer: KafkaProducer[F, OrderID, Order],
      config: KafkaConfig
  ) extends OrderFromPantryProducer[F] {

    private val Topic = config.topics.orderFromPantry

    def produce(order: Order): EitherT[F, String, Unit] =
      producer
        .produce(ProducerRecords.one(ProducerRecord(Topic, order.id, order)))
        .flatten
        .attemptT
        .leftMap(_.getMessage)
        .void
  }
}
