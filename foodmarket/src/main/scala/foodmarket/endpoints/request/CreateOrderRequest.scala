package foodmarket.endpoints.request

import common.model.ProductID
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class CreateOrderRequest(productIds: Seq[ProductID])

object CreateOrderRequest {
  implicit val codec: Codec[CreateOrderRequest] = deriveCodec[CreateOrderRequest]
  implicit val schema: Schema[CreateOrderRequest] = Schema.derived[CreateOrderRequest]
}
