package delivery.repository

import common.model.Courier
import doobie.Meta
import doobie.postgres.circe.jsonb.implicits.{pgDecoderGet, pgEncoderPut}

object DoobieInstances {
  implicit val productMeta: Meta[Courier] = new Meta(pgDecoderGet, pgEncoderPut)
}
