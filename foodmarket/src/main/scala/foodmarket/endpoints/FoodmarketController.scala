package foodmarket.endpoints

import cats.Functor
import common.model.OrderStatus.OrderStatus
import common.model.{Order, OrderID}
import foodmarket.endpoints.request.CreateOrderRequest
import foodmarket.endpoints.response.GetOrderStatusResponse
import foodmarket.services.FoodmarketService
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

//noinspection MutatorLikeMethodIsParameterless
final class FoodmarketController[F[_]: Functor](foodmarketService: FoodmarketService[F]) extends Controller[F] {

  private def createOrder: ServerEndpoint[Any, F] =
    endpoint
      .post
      .in("api" / "v1" / "order" / "create")
      .in(jsonBody[CreateOrderRequest])
      .out(jsonBody[Order])
      .errorOut(stringBody)
      .serverLogic(foodmarketService.createOrder(_).value)

  private def getOrder: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "order" / path[OrderID]("order-id"))
      .out(jsonBody[Order])
      .errorOut(stringBody)
      .serverLogic(foodmarketService.getOrder(_).value)

  private def getOrderStatus: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "order" / path[OrderID]("order-id") / "status")
      .out(jsonBody[GetOrderStatusResponse])
      .errorOut(stringBody)
      .serverLogic(foodmarketService.getOrderStatus(_).value)

  def endpoints: List[ServerEndpoint[Any, F]] =
    List(createOrder, getOrder, getOrderStatus).map(_.tag("foodmarket"))
}

object FoodmarketController {
  def impl[F[_]: Functor](foodmarketService: FoodmarketService[F]): FoodmarketController[F] =
    new FoodmarketController[F](foodmarketService)
}
