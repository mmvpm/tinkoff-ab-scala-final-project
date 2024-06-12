package pantry.endpoints.request

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class CreateProductRequest(name: String)

object CreateProductRequest {
  implicit val codec: Codec[CreateProductRequest] = deriveCodec[CreateProductRequest]
  implicit val schema: Schema[CreateProductRequest] = Schema.derived[CreateProductRequest]
}
