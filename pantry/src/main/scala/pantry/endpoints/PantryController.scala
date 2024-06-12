package pantry.endpoints

import cats.Functor
import common.model.{Product, ProductID}
import pantry.endpoints.request.CreateProductRequest
import pantry.services.PantryService
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

//noinspection MutatorLikeMethodIsParameterless
final class PantryController[F[_]: Functor](pantryService: PantryService[F]) extends Controller[F] {

  private def getAllProducts: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "product" / "list")
      .out(jsonBody[GetAllProductsResponse])
      .errorOut(stringBody)
      .serverLogic(_ => pantryService.getAllProducts.value)

  private def createProduct: ServerEndpoint[Any, F] =
    endpoint
      .post
      .in("api" / "v1" / "product" / "create")
      .in(jsonBody[CreateProductRequest])
      .out(jsonBody[Product])
      .errorOut(stringBody)
      .serverLogic(pantryService.createProduct(_).value)

  private def getProduct: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "product" / path[ProductID]("product-id"))
      .out(jsonBody[Product])
      .errorOut(stringBody)
      .serverLogic(pantryService.getProduct(_).value)

  private def updateProductCount: ServerEndpoint[Any, F] =
    endpoint
      .put
      .in("api" / "v1" / "product" / path[ProductID]("product-id") / "amount")
      .description("Increase product's count by the specified amount (decrease if negative) and returns a new value")
      .in(query[Int]("amount"))
      .out(stringBody)
      .errorOut(stringBody)
      .serverLogic { case (productId, amount) =>
        pantryService.updateProductCount(productId, amount).map(_.toString).value
      }

  private def getProductCount: ServerEndpoint[Any, F] =
    endpoint
      .get
      .in("api" / "v1" / "product" / path[ProductID]("product-id") / "amount")
      .out(stringBody)
      .errorOut(stringBody)
      .serverLogic(pantryService.getProductCount(_).map(_.toString).value)

  def endpoints: List[ServerEndpoint[Any, F]] =
    List(getAllProducts, createProduct, getProduct, updateProductCount, getProductCount).map(_.tag("pantry"))
}

object PantryController {
  def impl[F[_]: Functor](pantryService: PantryService[F]): PantryController[F] =
    new PantryController[F](pantryService)
}
