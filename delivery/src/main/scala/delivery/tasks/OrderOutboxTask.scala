package delivery.tasks

import cats.data.EitherT
import cats.effect.Temporal
import cats.implicits._
import delivery.producers.OrderFromDeliveryProducer
import delivery.repository.DeliveryRepository
import delivery.tasks.OrderOutboxTask.Delay
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class OrderOutboxTask[F[_]: Temporal: Logger](
    deliveryRepository: DeliveryRepository[F],
    orderFromDeliveryProducer: OrderFromDeliveryProducer[F]
) {

  def run: F[Unit] =
    runOnce.foreverM

  private def runOnce: F[Unit] =
    (for {
      orders <- deliveryRepository.getOrderOutbox
      _ <- orders.toList.toNel match {
        case Some(ordersNel) =>
          for {
            _ <- EitherT.right[String](info"Found ${orders.size} orders to send")
            _ <- orders.traverse { case (orderId, courierId) =>
              orderFromDeliveryProducer.produce(orderId, courierId)
            }
            _ <- EitherT.right[String](info"Sent ${orders.size} orders to foodmarket")
            _ <- deliveryRepository.deleteOrderOutbox(ordersNel.map(_._1))
          } yield ()
        case None =>
          EitherT.pure[F, String](())
      }
      _ <- EitherT.right[String](Temporal[F].sleep(Delay))
    } yield ())
      .value
      .map {
        case Left(error) => error"OrderOutboxTask failed with error: $error"
        case _           => ()
      }
}

object OrderOutboxTask {
  val Delay: FiniteDuration = 1.second
}
