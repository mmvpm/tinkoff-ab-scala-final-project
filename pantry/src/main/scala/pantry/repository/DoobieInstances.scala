package pantry.repository

import common.model._
import doobie.Meta
import doobie.postgres.circe.jsonb.implicits.{pgDecoderGet, pgEncoderPut}

object DoobieInstances {
  implicit val orderMeta: Meta[Order] = new Meta(pgDecoderGet, pgEncoderPut)
  implicit val productMeta: Meta[Product] = new Meta(pgDecoderGet, pgEncoderPut)
}
