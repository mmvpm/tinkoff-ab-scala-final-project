package delivery.endpoints

import cats.Functor
import common.model.{Courier, CourierID}
import delivery.endpoints.request.CreateCourierRequest
import delivery.services.DeliveryService
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

//noinspection MutatorLikeMethodIsParameterless
final class DeliveryController[F[_]: Functor](deliveryService: DeliveryService[F]) extends Controller[F] {

  private def createCourier: ServerEndpoint[Any, F] =
    endpoint
      .post
      .in("api" / "v1" / "courier" / "create")
      .in(jsonBody[CreateCourierRequest])
      .out(jsonBody[Courier])
      .errorOut(stringBody)
      .serverLogic(deliveryService.createCourier(_).value)

  private def getCourier: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "courier" / path[CourierID]("courier-id"))
      .out(jsonBody[Courier])
      .errorOut(stringBody)
      .serverLogic(deliveryService.getCourier(_).value)

  private def updateCourierAvailability: ServerEndpoint[Any, F] =
    endpoint
      .put
      .in("api" / "v1" / "courier" / path[CourierID]("courier-id") / "availability")
      .in(query[Boolean]("is-available"))
      .errorOut(stringBody)
      .serverLogic { case (courierId, isAvailable) =>
        deliveryService.updateCourierAvailability(courierId, isAvailable).value
      }

  private def getCourierAvailability: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "courier" / path[CourierID]("courier-id") / "availability")
      .out(stringBody)
      .errorOut(stringBody)
      .serverLogic(deliveryService.getCourierAvailability(_).map(_.toString).value)

  def endpoints: List[ServerEndpoint[Any, F]] =
    List(createCourier, getCourier, updateCourierAvailability, getCourierAvailability).map(_.tag("delivery"))
}

object DeliveryController {
  def impl[F[_]: Functor](deliveryService: DeliveryService[F]): DeliveryController[F] =
    new DeliveryController[F](deliveryService)
}
