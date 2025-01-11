package delivery.endpoints.request

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class CreateCourierRequest(name: String)

object CreateCourierRequest {
  implicit val codec: Codec[CreateCourierRequest] = deriveCodec[CreateCourierRequest]
  implicit val schema: Schema[CreateCourierRequest] = Schema.derived[CreateCourierRequest]
}
