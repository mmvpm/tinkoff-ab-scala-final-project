package delivery

import cats.effect.{Async, Resource}
import cats.syntax.option._
import delivery.config.PostgresConfig
import doobie.hikari.{Config, HikariTransactor}

package object database {

  def makeTransactor[F[_]: Async](
      config: PostgresConfig
  ): Resource[F, HikariTransactor[F]] = {
    val hikariConfig = Config(
      jdbcUrl = config.url,
      username = config.user.some,
      password = config.password.some,
      maximumPoolSize = config.poolSize,
      driverClassName = "org.postgresql.Driver".some
    )

    HikariTransactor.fromConfig[F](hikariConfig)
  }

}
