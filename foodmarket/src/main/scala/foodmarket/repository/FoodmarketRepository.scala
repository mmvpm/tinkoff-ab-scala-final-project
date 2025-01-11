package foodmarket.repository

import cats.data.{EitherT, NonEmptyList}
import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import common.model.OrderStatus.OrderStatus
import common.model._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.in
import foodmarket.repository.DoobieInstances._

trait FoodmarketRepository[F[_]] {

  def createOrder(order: Order): EitherT[F, String, Unit]
  def getOrder(orderId: OrderID): EitherT[F, String, Option[Order]]

  def setOrderStatus(orderId: OrderID, status: OrderStatus): EitherT[F, String, Option[Unit]]
  def getOrderStatus(orderId: OrderID): EitherT[F, String, Option[OrderStatus]]

  def createFailedOrder(orderId: OrderID, reason: String): EitherT[F, String, Unit]
  def getFailedOrderReason(orderId: OrderID): EitherT[F, String, Option[String]]

  def getOrderOutbox: EitherT[F, String, Vector[Order]]
  def deleteOrderOutbox(orderIds: NonEmptyList[OrderID]): EitherT[F, String, Unit]
}

object FoodmarketRepository {

  def impl[F[_]: Sync](xa: Transactor[F]): FoodmarketRepository[F] =
    new Impl[F](xa)

  final private class Impl[F[_]: Sync](xa: Transactor[F]) extends FoodmarketRepository[F] {

    def createOrder(order: Order): EitherT[F, String, Unit] = {
      sql"""
        |insert into orders (id, order_raw) values (${order.id}, $order);
        |insert into order_status (order_id, status) values (${order.id}, ${OrderStatus.Pending});
        |insert into order_outbox (order_id) values (${order.id});
        |""".stripMargin
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .void
    }

    def getOrder(orderId: OrderID): EitherT[F, String, Option[Order]] =
      sql"select order_raw from orders where id = $orderId"
        .query[Order]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def setOrderStatus(orderId: OrderID, status: OrderStatus.OrderStatus): EitherT[F, String, Option[Unit]] =
      sql"update order_status set status = $status where order_id = $orderId"
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .map(res => Option.when(res == 1)(()))

    def getOrderStatus(orderId: OrderID): EitherT[F, String, Option[OrderStatus]] =
      sql"select status from order_status where order_id = $orderId"
        .query[OrderStatus]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def createFailedOrder(orderId: OrderID, reason: String): EitherT[F, String, Unit] =
      sql"insert into failed_orders (order_id, reason) values ($orderId, $reason)"
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .void

    def getFailedOrderReason(orderId: OrderID): EitherT[F, String, Option[String]] =
      sql"select reason from failed_orders where order_id = $orderId"
        .query[String]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def getOrderOutbox: EitherT[F, String, Vector[Order]] =
      sql"select order_raw from order_outbox join orders o on order_outbox.order_id = o.id"
        .query[Order]
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
  }
}
