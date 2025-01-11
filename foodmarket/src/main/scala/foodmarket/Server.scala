package foodmarket

import cats.effect.{Async, IO}
import com.comcast.ip4s._
import foodmarket.config.AppConfig
import foodmarket.consumers.{OrderFromDeliveryConsumer, OrderFromPantryFailedConsumer}
import foodmarket.database.{FlywayMigration, makeTransactor}
import foodmarket.endpoints.FoodmarketController
import foodmarket.metrics.ServerMetrics
import foodmarket.producers.OrderFromFoodmarketProducer
import foodmarket.repository.FoodmarketRepository
import foodmarket.services.FoodmarketService
import foodmarket.tasks.OrderOutboxTask
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j._
import org.typelevel.log4cats.syntax._
import org.typelevel.log4cats.{Logger, LoggerFactory}
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
      pantryRepository = FoodmarketRepository.impl[IO](xa)

      orderFromPantryProducer <- OrderFromFoodmarketProducer.impl[IO](config.kafka)

      orderFromDeliveryConsumer <- OrderFromDeliveryConsumer.impl[IO](config.kafka, pantryRepository)
      _ <- Async[IO].background(orderFromDeliveryConsumer.run(config.kafka))

      orderFromPantryFailedConsumer <- OrderFromPantryFailedConsumer.impl[IO](config.kafka, pantryRepository)
      _ <- Async[IO].background(orderFromPantryFailedConsumer.run(config.kafka))

      orderOutboxTask = new OrderOutboxTask[IO](pantryRepository, orderFromPantryProducer)
      _ <- Async[IO].background(orderOutboxTask.run)
    } yield pantryRepository).use { pantryRepository =>
      for {
        //_ <- FlywayMigration.clear[IO](config.database)
        _ <- FlywayMigration.migrate[IO](config.database)

        pantryService = FoodmarketService.impl[IO](pantryRepository)

        pantryController = FoodmarketController.impl[IO](pantryService)

        endpoints = List(
          pantryController.endpoints,
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
        ).fromServerEndpoints[IO](endpoints, "pantry-api", "1.0.0")

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
