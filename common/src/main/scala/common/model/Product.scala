package common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class Product(id: ProductID, name: String)

object Product {
  implicit val codec: Codec[Product] = deriveCodec[Product]
  implicit val schema: Schema[Product] = Schema.derived[Product]
}
