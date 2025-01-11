package foodmarket.repository

import common.model.OrderStatus.OrderStatus
import common.model.{Order, OrderStatus}
import doobie.Meta
import doobie.postgres.circe.jsonb.implicits.{pgDecoderGet, pgEncoderPut}

object DoobieInstances {
  implicit val orderMeta: Meta[Order] = new Meta(pgDecoderGet, pgEncoderPut)
  implicit val orderStatusMeta: Meta[OrderStatus] = Meta[String].timap(OrderStatus.withName)(_.toString)
}
