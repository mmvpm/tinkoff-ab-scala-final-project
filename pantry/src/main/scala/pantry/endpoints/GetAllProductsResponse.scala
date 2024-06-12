package pantry.endpoints

import common.model._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import pantry.endpoints.GetAllProductsResponse.RichProduct
import sttp.tapir.Schema

final case class GetAllProductsResponse(products: Seq[RichProduct])

object GetAllProductsResponse {

  final case class RichProduct(product: Product, count: Int)

  implicit val codecRichProduct: Codec[RichProduct] = deriveCodec[RichProduct]
  implicit val codec: Codec[GetAllProductsResponse] = deriveCodec[GetAllProductsResponse]

  implicit val schemaRichProduct: Schema[RichProduct] = Schema.derived[RichProduct]
  implicit val schema: Schema[GetAllProductsResponse] = Schema.derived[GetAllProductsResponse]
}
