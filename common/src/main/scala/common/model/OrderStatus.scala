package common.model

import io.circe.Codec
import sttp.tapir.Schema

//noinspection TypeAnnotation
object OrderStatus extends Enumeration {
  type OrderStatus = Value
  val Pending = Value("Pending")
  val Completed = Value("Completed")
  val Failed = Value("Failed")

  implicit val codec: Codec[OrderStatus] = Codec.codecForEnumeration(OrderStatus)
  implicit val schema: Schema[OrderStatus] = Schema.derivedEnumerationValue[OrderStatus]
}
