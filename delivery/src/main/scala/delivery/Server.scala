package delivery

import cats.effect.{Async, IO}
import com.comcast.ip4s._
import delivery.config.AppConfig
import delivery.consumers.OrderFromPantryConsumer
import delivery.database.FlywayMigration
import delivery.endpoints.DeliveryController
import delivery.producers.{OrderFromDeliveryFailedProducer, OrderFromDeliveryProducer}
import delivery.repository.DeliveryRepository
import delivery.services.DeliveryService
import delivery.tasks.OrderOutboxTask
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j._
import org.typelevel.log4cats.syntax._
import org.typelevel.log4cats.{Logger, LoggerFactory}
import delivery.database.makeTransactor
import delivery.metrics.ServerMetrics
import io.prometheus.metrics.model.registry.PrometheusRegistry
import pureconfig.ConfigSource
import pureconfig.generic.auto.exportReader
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Server {

  def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]

    implicit val logger: Logger[IO] = LoggerFactory[IO].getLogger

    val prometheusRegistry = PrometheusRegistry.defaultRegistry
    val prometheusMetrics = ServerMetrics.register[IO](prometheusRegistry)

    (for {
      xa <- makeTransactor[IO](config.database)
      deliveryRepository = DeliveryRepository.impl[IO](xa)

      orderFromDeliveryProducer <- OrderFromDeliveryProducer.impl[IO](config.kafka)
      orderFromDeliveryFailedProducer <- OrderFromDeliveryFailedProducer.impl[IO](config.kafka)

      orderFromFoodmarketConsumer <- OrderFromPantryConsumer.impl[IO](
        config.kafka,
        deliveryRepository,
        orderFromDeliveryFailedProducer
      )
      _ <- Async[IO].background(orderFromFoodmarketConsumer.run(config.kafka))

      orderOutboxTask = new OrderOutboxTask[IO](deliveryRepository, orderFromDeliveryProducer)
      _ <- Async[IO].background(orderOutboxTask.run)
    } yield deliveryRepository).use { deliveryRepository =>
      for {
        //_ <- FlywayMigration.clear[IO](config.database)
        _ <- FlywayMigration.migrate[IO](config.database)

        deliveryService = DeliveryService.impl[IO](deliveryRepository)

        deliveryController = DeliveryController.impl[IO](deliveryService)

        endpoints = List(
          deliveryController.endpoints,
          List(prometheusMetrics.metricsEndpoint)
        ).flatten

        swagger = SwaggerInterpreter(
          swaggerUIOptions = SwaggerUIOptions(
            pathPrefix = List("swagger"),
            yamlName = "swagger.yaml",
            contextPath = List(),
            useRelativePaths = false,
            showExtensions = false
          )
        ).fromServerEndpoints[IO](endpoints, "delivery-api", "1.0.0")

        serverOptions = Http4sServerOptions.customiseInterceptors[IO]
          .metricsInterceptor(prometheusMetrics.metricsInterceptor(ignoreEndpoints = swagger.map(_.endpoint)))
          .options

        httpApp = Http4sServerInterpreter[IO](serverOptions)
          .toRoutes(swagger ++ endpoints)
          .orNotFound

        host <- IO.fromOption(Host.fromString(config.http.host))(new RuntimeException("Invalid http host"))
        port <- IO.fromOption(Port.fromInt(config.http.port))(new RuntimeException("Invalid http port"))

        _ <- debug"Go to http://${config.http.host}:${config.http.port}/swagger to open SwaggerUI"

        _ <- EmberServerBuilder.default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpApp(httpApp)
          .build
          .useForever
      } yield ()
    }
  }
}
