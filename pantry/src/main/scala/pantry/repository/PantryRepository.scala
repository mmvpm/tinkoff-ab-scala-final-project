package pantry.repository

import cats.data.{EitherT, NonEmptyList}
import cats.effect.Sync
import cats.syntax.list._
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps, toTraverseOps}
import common.model._
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.in
import pantry.repository.DoobieInstances._
import pantry.repository.model.ProductCountEntry

trait PantryRepository[F[_]] {

  def createProduct(product: Product): EitherT[F, String, Unit]
  def getProduct(productId: ProductID): EitherT[F, String, Option[Product]]
  def getAllProducts: EitherT[F, String, Vector[Product]]

  def updateProductCount(productId: ProductID, amount: Int): EitherT[F, String, Option[Int]]
  def getProductCount(productId: ProductID): EitherT[F, String, Option[Int]]
  def getProductCountBatch(productIds: NonEmptyList[ProductID]): EitherT[F, String, Vector[ProductCountEntry]]

  def registerOrder(order: Order): EitherT[F, String, Unit]
  def getOrder(orderId: OrderID): EitherT[F, String, Option[Order]]

  def getOrderOutbox: EitherT[F, String, Vector[Order]]
  def deleteOrderOutbox(orderIds: NonEmptyList[OrderID]): EitherT[F, String, Unit]
}

object PantryRepository {

  final private class Impl[F[_]: Sync](xa: Transactor[F]) extends PantryRepository[F] {

    def createProduct(product: Product): EitherT[F, String, Unit] = {
      sql"""
        |insert into products (id, product) values (${product.id}, $product);
        |insert into product_count (product_id, count) values (${product.id}, 0);
        |""".stripMargin
        .update.run
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)
        .void
    }

    def getProduct(productId: ProductID): EitherT[F, String, Option[Product]] =
      sql"select product from products where id = $productId"
        .query[Product]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def getAllProducts: EitherT[F, String, Vector[Product]] =
      sql"select product from products"
        .query[Product]
        .to[Vector]
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def updateProductCount(productId: ProductID, amount: Int): EitherT[F, String, Option[Int]] =
      sql"update product_count set count = count + $amount where product_id = $productId returning count"
        .query[Int]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def getProductCount(productId: ProductID): EitherT[F, String, Option[Int]] =
      sql"select count from product_count where product_id = $productId"
        .query[Int]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def getProductCountBatch(productIds: NonEmptyList[ProductID]): EitherT[F, String, Vector[ProductCountEntry]] =
      selectProductCountBatch(productIds)
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def registerOrder(order: Order): EitherT[F, String, Unit] =
      (for {
        productsNel <- EitherT.fromOption[ConnectionIO](
          order.products.toList.toNel,
          s"order ${order.id} has no products to buy"
        )
        _ <- EitherT(checkProductsAvailability(productsNel))
        _ <- EitherT.right[String](buyProducts(order.products))
        _ <- EitherT.right[String](insertIntoOrders(order))
        _ <- EitherT.right[String](insertIntoOrdersOutbox(order))
      } yield ())
        .transact(xa)

    def getOrder(orderId: OrderID): EitherT[F, String, Option[Order]] =
      sql"select order_raw from orders where order_id = $orderId"
        .query[Order]
        .option
        .transact(xa)
        .attemptT
        .leftMap(_.getMessage)

    def getOrderOutbox: EitherT[F, String, Vector[Order]] =
      sql"select order_raw from order_outbox join orders o on order_outbox.order_id = o.order_id"
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

    // register order

    private def selectProductCountBatch(productIds: NonEmptyList[ProductID]): ConnectionIO[Vector[ProductCountEntry]] =
      (fr"select product_id, count from product_count where " ++ in(fr"product_id", productIds))
        .query[ProductCountEntry]
        .to[Vector]

    private def checkProductsAvailability(productIds: NonEmptyList[ProductID]): ConnectionIO[Either[String, Unit]] =
      selectProductCountBatch(productIds)
        .map { actualCounts =>
          val nonExistentProducts = productIds.toList.toSet diff actualCounts.map(_.productId).toSet
          val neededCounts = productIds.groupBy(identity).view.mapValues(_.length)
          val unavailableProducts = actualCounts.collect {
            case entry if entry.count < neededCounts(entry.productId) => entry.productId
          } ++ nonExistentProducts
          Either.cond(
            unavailableProducts.isEmpty,
            (),
            s"product(s) ${unavailableProducts.mkString(", ")} unavailable in sufficient quantity or not exist"
          )
        }

    private def buyProducts(productIds: Seq[ProductID]): ConnectionIO[Unit] =
      productIds.traverse { productId =>
        sql"update product_count set count = count - 1 where product_id = $productId".update.run
      }.void

    private def insertIntoOrders(order: Order): ConnectionIO[Unit] =
      sql"insert into orders(order_id, order_raw) VALUES (${order.id}, $order)".update.run.void

    private def insertIntoOrdersOutbox(order: Order): ConnectionIO[Unit] =
      sql"insert into order_outbox(order_id) VALUES (${order.id})".update.run.void
  }

  def impl[F[_]: Sync](xa: Transactor[F]): PantryRepository[F] =
    new Impl[F](xa)
}
