import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.0.1-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "final-project"
  )

lazy val common = (project in file("common"))
  .settings(
    name := "common",
    libraryDependencies ++= Pantry.dependencies,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )

lazy val pantry = (project in file("pantry"))
  .dependsOn(common)
  .settings(
    name := "pantry",
    libraryDependencies ++= Pantry.dependencies,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )

lazy val delivery = (project in file("delivery"))
  .dependsOn(common)
  .settings(
    name := "delivery",
    libraryDependencies ++= Delivery.dependencies,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )

lazy val foodmarket = (project in file("foodmarket"))
  .dependsOn(common)
  .settings(
    name := "foodmarket",
    libraryDependencies ++= Foodmarket.dependencies,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )
