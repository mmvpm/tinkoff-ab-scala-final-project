package foodmarket.endpoints.response

import common.model.OrderID
import common.model.OrderStatus.OrderStatus
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class GetOrderStatusResponse(
    orderId: OrderID,
    status: OrderStatus,
    info: Option[String]
)

final object GetOrderStatusResponse {
  implicit val codec: Codec[GetOrderStatusResponse] = deriveCodec[GetOrderStatusResponse]
  implicit val schema: Schema[GetOrderStatusResponse] = Schema.derived[GetOrderStatusResponse]
}
