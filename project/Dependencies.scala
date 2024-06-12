import sbt.*

trait Dependencies {
  def dependencies: Seq[ModuleID]
}

object Dependencies {

  object Common extends Dependencies {
    def dependencies: Seq[sbt.ModuleID] = Seq(
      Libs.circe,
      Libs.tapir,
    ).flatten
  }

  object Pantry extends Dependencies {
    def dependencies: Seq[sbt.ModuleID] = Seq(
      Libs.cats,
      Libs.logback,
      Libs.log4cats,
      Libs.circe,
      Libs.pureconfig,
      Libs.doobie,
      Libs.flyway,
      Libs.sttp,
      Libs.http4s,
      Libs.tapir,
      Libs.prometheus,
      Libs.fs2
    ).flatten
  }

  object Delivery extends Dependencies {
    def dependencies: Seq[sbt.ModuleID] = Seq(
      Libs.cats,
      Libs.logback,
      Libs.log4cats,
      Libs.circe,
      Libs.pureconfig,
      Libs.doobie,
      Libs.flyway,
      Libs.sttp,
      Libs.http4s,
      Libs.tapir,
      Libs.prometheus,
      Libs.fs2
    ).flatten
  }

  object Foodmarket extends Dependencies {
    def dependencies: Seq[sbt.ModuleID] = Seq(
      Libs.cats,
      Libs.logback,
      Libs.log4cats,
      Libs.circe,
      Libs.pureconfig,
      Libs.doobie,
      Libs.flyway,
      Libs.sttp,
      Libs.http4s,
      Libs.tapir,
      Libs.prometheus,
      Libs.fs2
    ).flatten
  }
}
