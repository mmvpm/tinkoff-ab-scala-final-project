package delivery.repository

import cats.data.{EitherT, NonEmptyList}
import cats.effect.Sync
import cats.syntax.list._
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps, toTraverseOps}
import common.model._
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.in
import DoobieInstances._

trait DeliveryRepository[F[_]] {

  def createCourier(courier: Courier): EitherT[F, String, Unit]
  def getCourier(courierId: CourierID): EitherT[F, String, Option[Courier]]

  def updateCourierAvailability(courierId: CourierID, isAvailable: Boolean): EitherT[F, String, Option[Unit]]
  def getCourierAvailability(courierId: CourierID): EitherT[F, String, Option[Boolean]]

  def registerOrder(order: Order): EitherT[F, String, Unit]

  def getOrderOutbox: EitherT[F, String, Vector[(OrderID, CourierID)]]
  def deleteOrderOutbox(orderIds: NonEmptyList[OrderID]): EitherT[F, String, Unit]
}

object DeliveryRepository {

  final private class Impl[F[_]: Sync](xa: Transactor[F]) extends DeliveryRepository[F] {

    def createCourier(courier: Courier): EitherT[F, String, Unit] = {
      sql"""
        |insert into couriers (id, courier) values (${courier.id}, $courier);
        |insert into courier_availability (courier_id, is_available) values (${courier.id}, true);
        |""".stripMargin
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .void
    }

    def getCourier(courierId: CourierID): EitherT[F, String, Option[Courier]] =
      sql"select courier from couriers where id = $courierId"
        .query[Courier]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def updateCourierAvailability(courierId: CourierID, isAvailable: Boolean): EitherT[F, String, Option[Unit]] =
      sql"update courier_availability set is_available = $isAvailable where courier_id = $courierId"
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .map(res => Option.when(res == 1)(()))

    def getCourierAvailability(courierId: CourierID): EitherT[F, String, Option[Boolean]] =
      sql"select is_available from courier_availability where courier_id = $courierId"
        .query[Boolean]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def registerOrder(order: Order): EitherT[F, String, Unit] =
      (for {
        optCourierId <- EitherT.right[String](getAvailableCourier)
        courierId <- EitherT.fromOption[ConnectionIO](optCourierId, s"no courier is available")
        _ <- EitherT.right[String](markCourierBusy(courierId))
        _ <- EitherT.right[String](insertIntoOrderCourier(order.id, courierId))
        _ <- EitherT.right[String](insertIntoOrdersOutbox(order.id))
      } yield ())
        .transact(xa)

    def getOrderOutbox: EitherT[F, String, Vector[(OrderID, CourierID)]] =
      sql"select o.order_id, courier_id from order_outbox join order_courier o on order_outbox.order_id = o.order_id"
        .query[(OrderID, CourierID)]
        .to[Vector]
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def deleteOrderOutbox(orderIds: NonEmptyList[OrderID]): EitherT[F, String, Unit] =
      (fr"delete from order_outbox where " ++ in(fr"order_id", orderIds))
        .update
        .run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .void

    // register order

    private def getAvailableCourier: ConnectionIO[Option[CourierID]] =
      sql"select courier_id from courier_availability where is_available = true limit 1"
        .query[CourierID]
        .option

    private def markCourierBusy(courierId: CourierID): ConnectionIO[Unit] =
      sql"update courier_availability set is_available = false where courier_id = $courierId".update.run.void

    private def insertIntoOrderCourier(orderId: OrderID, courierId: CourierID): ConnectionIO[Unit] =
      sql"insert into order_courier(order_id, courier_id) VALUES ($orderId, $courierId)".update.run.void

    private def insertIntoOrdersOutbox(orderId: OrderID): ConnectionIO[Unit] =
      sql"insert into order_outbox(order_id) VALUES ($orderId)".update.run.void
  }

  def impl[F[_]: Sync](xa: Transactor[F]): DeliveryRepository[F] =
    new Impl[F](xa)
}
