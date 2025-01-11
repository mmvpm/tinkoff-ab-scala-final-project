package pantry.config

final case class PostgresConfig(
    url: String,
    user: String,
    password: String,
    poolSize: Int
)
