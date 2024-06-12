package common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class Order(id: OrderID, products: Seq[ProductID])

object Order {
  implicit val codec: Codec[Order] = deriveCodec[Order]
  implicit val schema: Schema[Order] = Schema.derived[Order]
}
