
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "acme",
    idePackagePrefix := Some("com.acme")
  )  .settings(
  Compile / mainClass := Some("com.acme.Main"),
  Compile / discoveredMainClasses := Seq(),
  organization := "Acme",
  name := "Acme Factory",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.8",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.4.2",
    "ch.qos.logback" % "logback-classic" % "1.2.11",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",

    "org.typelevel" %% "cats-effect-kernel" % "3.3.11",
    "org.typelevel" %% "cats-effect-std" % "3.3.11",
    "org.typelevel" %% "cats-effect" % "3.3.11",

    "org.scalatest" %% "scalatest" % "3.2.12" % "test",
  )
)

