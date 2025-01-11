import sbt._

object Libs {

  private object V {
    val pureconfigVersion = "0.17.5"
    val catsCoreVersion = "2.10.0"
    val catsEffectVersion = "3.5.3"
    val catsTaglessVersion = "0.15.0"
    val http4sVersion = "0.23.25"
    val tapirVersion = "1.9.9"
    val sttpVersion = "3.9.3"
    val fs2Version = "3.3.1"
    val s3Version = "2.25.35"
    val logbackVersion = "1.4.14"
    val log4catsVersion = "2.6.0"
    val prometheusVersion = "0.16.0"
    val enumeratumVersion = "1.7.3"
    val enumeratumDoobieVersion = "1.7.4"
    val circeVersion = "0.14.6"
    val phobosVersion = "0.21.0"
    val newtypeVersion = "0.4.4"
    val doobieVersion = "1.0.0-RC4"
    val flywayVersion = "9.17.0"
    val redis4catsVersion = "1.5.2"
    val scalatestVersion = "3.2.18"
    val testcontainersVersion = "0.41.3"
  }

  val pureconfig: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % V.pureconfigVersion
  )

  val cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % V.catsCoreVersion,
    "org.typelevel" %% "cats-effect" % V.catsEffectVersion,
    "org.typelevel" %% "cats-tagless-macros" % V.catsTaglessVersion
  )

  val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-ember-client" % V.http4sVersion,
    "org.http4s" %% "http4s-ember-server" % V.http4sVersion,
    "org.http4s" %% "http4s-dsl" % V.http4sVersion,
    "org.http4s" %% "http4s-circe" % V.http4sVersion
  )

  val tapir: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % V.tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % V.tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % V.tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapirVersion
  )

  val sttp: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.client3" %% "core" % V.sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe" % V.sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % V.sttpVersion,
    "com.softwaremill.sttp.client3" %% "prometheus-backend" % V.sttpVersion
  )

  val fs2: Seq[ModuleID] = Seq(
    "com.github.fd4s" %% "fs2-kafka" % V.fs2Version
  )

  val s3: Seq[ModuleID] = Seq(
    "software.amazon.awssdk" % "s3" % V.s3Version
  )

  val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % V.logbackVersion
  )

  val log4cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "log4cats-core" % V.log4catsVersion,
    "org.typelevel" %% "log4cats-slf4j" % V.log4catsVersion
  )

  val prometheus: Seq[ModuleID] = Seq(
    "io.prometheus" % "simpleclient_common" % V.prometheusVersion,
    "io.prometheus" % "simpleclient_hotspot" % V.prometheusVersion
  )

  val enumeratum: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % V.enumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % V.enumeratumVersion,
    "com.beachape" %% "enumeratum-doobie" % V.enumeratumDoobieVersion
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic" % V.circeVersion
  )

  val phobos: Seq[ModuleID] = Seq(
    "ru.tinkoff" %% "phobos-core" % V.phobosVersion
  )

  val newtype: Seq[ModuleID] = Seq(
    "io.estatico" %% "newtype" % V.newtypeVersion
  )

  val doobie: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-core" % V.doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % V.doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % V.doobieVersion,
    "org.tpolecat" %% "doobie-postgres-circe" % V.doobieVersion
  )

  val flyway: Seq[ModuleID] = Seq(
    "org.flywaydb" % "flyway-core" % V.flywayVersion
  )

  val redis4cats: Seq[ModuleID] = Seq(
    "dev.profunktor" %% "redis4cats-effects" % V.redis4catsVersion,
    "dev.profunktor" %% "redis4cats-log4cats" % V.redis4catsVersion
  )

  val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % V.scalatestVersion
  )

  val testcontainers: Seq[ModuleID] = Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest" % V.testcontainersVersion,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testcontainersVersion,
    "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersVersion
  )

}
