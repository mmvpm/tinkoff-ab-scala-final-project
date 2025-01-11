package foodmarket.config

final case class AppConfig(
    http: HttpServer,
    database: PostgresConfig,
    kafka: KafkaConfig
)
