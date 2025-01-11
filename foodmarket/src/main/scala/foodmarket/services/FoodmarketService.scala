package foodmarket.services

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import common.model.{Order, OrderID, OrderStatus}
import foodmarket.endpoints.request.CreateOrderRequest
import foodmarket.endpoints.response.GetOrderStatusResponse
import foodmarket.repository.FoodmarketRepository

trait FoodmarketService[F[_]] {
  def createOrder(request: CreateOrderRequest): EitherT[F, String, Order]
  def getOrder(id: OrderID): EitherT[F, String, Order]
  def getOrderStatus(id: OrderID): EitherT[F, String, GetOrderStatusResponse]
}

object FoodmarketService {

  final private class Impl[F[_]: Monad: UUIDGen](foodmarketRepository: FoodmarketRepository[F])
      extends FoodmarketService[F] {

    def createOrder(request: CreateOrderRequest): EitherT[F, String, Order] =
      for {
        uuid <- EitherT.liftF(UUIDGen[F].randomUUID)
        order = Order(uuid, request.productIds)
        _ <- foodmarketRepository.createOrder(order)
      } yield order

    def getOrder(id: OrderID): EitherT[F, String, Order] =
      for {
        optOrder <- foodmarketRepository.getOrder(id)
        order <- EitherT.fromOption[F](optOrder, s"order $id is not found")
      } yield order

    def getOrderStatus(id: OrderID): EitherT[F, String, GetOrderStatusResponse] =
      for {
        optStatus <- foodmarketRepository.getOrderStatus(id)
        status <- EitherT.fromOption[F](optStatus, s"order $id is not found")
        optFailureReason <- if (status == OrderStatus.Failed)
          foodmarketRepository.getFailedOrderReason(id)
        else
          EitherT.pure[F, String](Option.empty[String])
        response = GetOrderStatusResponse(id, status, optFailureReason)
      } yield response
  }

  def impl[F[_] : Monad : UUIDGen](foodmarketRepository: FoodmarketRepository[F]): FoodmarketService[F] =
    new Impl[F](foodmarketRepository)
}
