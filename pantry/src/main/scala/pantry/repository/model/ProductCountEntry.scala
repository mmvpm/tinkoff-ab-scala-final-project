package pantry.repository.model

import common.model.ProductID

final case class ProductCountEntry(productId: ProductID, count: Int)