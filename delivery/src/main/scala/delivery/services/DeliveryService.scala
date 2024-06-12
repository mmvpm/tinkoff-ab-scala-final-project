package delivery.services

import cats.Monad
import cats.data.EitherT
import cats.effect.std.UUIDGen
import common.model.{Courier, CourierID}
import delivery.endpoints.request.CreateCourierRequest
import delivery.repository.DeliveryRepository

trait DeliveryService[F[_]] {
  def createCourier(request: CreateCourierRequest): EitherT[F, String, Courier]
  def getCourier(id: CourierID): EitherT[F, String, Courier]
  def updateCourierAvailability(id: CourierID, isAvailable: Boolean): EitherT[F, String, Unit]
  def getCourierAvailability(id: CourierID): EitherT[F, String, Boolean]
}

object DeliveryService {

  final private class Impl[F[_]: Monad: UUIDGen](deliveryRepository: DeliveryRepository[F]) extends DeliveryService[F] {

    def createCourier(request: CreateCourierRequest): EitherT[F, String, Courier] =
      for {
        uuid <- EitherT.liftF(UUIDGen[F].randomUUID)
        courier = Courier(uuid, request.name)
        _ <- deliveryRepository.createCourier(courier)
      } yield courier

    def getCourier(id: CourierID): EitherT[F, String, Courier] =
      for {
        optCourier <- deliveryRepository.getCourier(id)
        courier <- EitherT.fromOption[F](optCourier, s"courier $id is not found")
      } yield courier

    def updateCourierAvailability(id: CourierID, isAvailable: Boolean): EitherT[F, String, Unit] =
      for {
        optSuccess <- deliveryRepository.updateCourierAvailability(id, isAvailable)
        _ <- EitherT.fromOption[F](optSuccess, s"courier $id is not found")
      } yield ()

    def getCourierAvailability(id: CourierID): EitherT[F, String, Boolean] =
      for {
        optCount <- deliveryRepository.getCourierAvailability(id)
        availability <- EitherT.fromOption[F](optCount, s"courier $id is not found")
      } yield availability
  }

  def impl[F[_]: Monad: UUIDGen](deliveryRepository: DeliveryRepository[F]): DeliveryService[F] =
    new Impl[F](deliveryRepository)
}
