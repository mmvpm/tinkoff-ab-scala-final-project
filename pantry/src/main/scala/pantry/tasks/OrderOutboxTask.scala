package pantry.tasks

import cats.data.EitherT
import cats.effect.Temporal
import cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import pantry.producers.OrderFromPantryProducer
import pantry.repository.PantryRepository
import pantry.tasks.OrderOutboxTask.Delay

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class OrderOutboxTask[F[_]: Temporal: Logger](
    pantryRepository: PantryRepository[F],
    orderFromPantryProducer: OrderFromPantryProducer[F]
) {

  def run: F[Unit] =
    runOnce.foreverM

  private def runOnce: F[Unit] =
    (for {
      orders <- pantryRepository.getOrderOutbox
      _ <- orders.toList.toNel match {
        case Some(ordersNel) =>
          for {
            _ <- EitherT.right[String](info"Found ${orders.size} orders to send")
            _ <- orders.traverse(orderFromPantryProducer.produce)
            _ <- EitherT.right[String](info"Sent ${orders.size} orders to delivery")
            _ <- pantryRepository.deleteOrderOutbox(ordersNel.map(_.id))
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
