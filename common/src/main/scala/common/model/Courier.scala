package common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

final case class Courier(id: CourierID, name: String)

object Courier {
  implicit val codec: Codec[Courier] = deriveCodec[Courier]
  implicit val schema: Schema[Courier] = Schema.derived[Courier]
}
