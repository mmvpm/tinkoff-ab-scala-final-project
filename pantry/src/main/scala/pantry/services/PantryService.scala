package pantry.services

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.syntax.list._
import common.model.{Product, ProductID}
import pantry.endpoints.GetAllProductsResponse
import pantry.endpoints.GetAllProductsResponse.RichProduct
import pantry.endpoints.request.CreateProductRequest
import pantry.repository.PantryRepository
import pantry.repository.model.ProductCountEntry

trait PantryService[F[_]] {
  def createProduct(request: CreateProductRequest): EitherT[F, String, Product]
  def getProduct(id: ProductID): EitherT[F, String, Product]
  def getAllProducts: EitherT[F, String, GetAllProductsResponse]
  def updateProductCount(id: ProductID, amount: Int): EitherT[F, String, Int]
  def getProductCount(id: ProductID): EitherT[F, String, Int]
}

object PantryService {

  final private class Impl[F[_]: Monad: UUIDGen](pantryRepository: PantryRepository[F]) extends PantryService[F] {

    def createProduct(request: CreateProductRequest): EitherT[F, String, Product] =
      for {
        uuid <- EitherT.liftF(UUIDGen[F].randomUUID)
        product = Product(uuid, request.name)
        _ <- pantryRepository.createProduct(product)
      } yield product

    def getProduct(id: ProductID): EitherT[F, String, Product] =
      for {
        optProduct <- pantryRepository.getProduct(id)
        product <- EitherT.fromOption[F](optProduct, s"product $id is not found")
      } yield product

    def updateProductCount(id: ProductID, amount: Int): EitherT[F, String, Int] =
      for {
        optNewCount <- pantryRepository.updateProductCount(id, amount)
        newCount <- EitherT.fromOption[F](optNewCount, s"product $id is not found")
      } yield newCount

    def getProductCount(id: ProductID): EitherT[F, String, Int] =
      for {
        optCount <- pantryRepository.getProductCount(id)
        count <- EitherT.fromOption[F](optCount, s"product $id is not found")
      } yield count

    def getAllProducts: EitherT[F, String, GetAllProductsResponse] =
      for {
        products <- pantryRepository.getAllProducts
        productsNel <- EitherT.fromOption[F](products.toList.toNel, "there are no products in the pantry")
        productCounts <- pantryRepository.getProductCountBatch(productsNel.map(_.id))
        response = makeGetAllProductsResponse(products, productCounts)
      } yield response
  }

  def impl[F[_]: Monad: UUIDGen](pantryRepository: PantryRepository[F]): PantryService[F] =
    new Impl[F](pantryRepository)

  // internal

  def makeGetAllProductsResponse(products: Seq[Product], counts: Seq[ProductCountEntry]): GetAllProductsResponse = {
    val countsMap = counts.map(entry => entry.productId -> entry.count).toMap
    val richProducts = products.map { product =>
      RichProduct(product, countsMap.getOrElse(product.id, 0))
    }
    GetAllProductsResponse(richProducts)
  }
}
