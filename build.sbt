
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

initialize := {
  val _ = initialize.value // run the previous initialization
  val required = "11"
  val current = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK $current. Use version $required instead.")
}

lazy val commonDockerSetting = Seq(
  dockerBaseImage := "openjdk:11-jre",
  dockerExposedPorts += 8080,
  dockerAliases := {
    val v = version.value

    val isSnapshot: Boolean = v.contains("SNAPSHOT")
    val maybeGitHash: Option[String] = git.gitHeadCommit.value

    (isSnapshot, maybeGitHash) match {
      case (false, _) => dockerAliases.value
      case (true, None) => Seq(dockerAlias.value.withTag(Some("dev")))
      case (true, Some(gitHash)) =>
        Seq(
          dockerAlias.value.withTag(Some(v.replaceAll("SNAPSHOT", gitHash.take(8)))),
          dockerAlias.value,
          dockerAlias.value.withTag(Some("dev"))
        )
    }
  },
  dockerUpdateLatest := !version.value.contains("SNAPSHOT"),
  Docker / packageName := packageName.value.split("-").last
)

lazy val root = (project in file("."))
  .settings(
    name := "acme",
    idePackagePrefix := Some("com.acme"))
  .settings(
  Compile / mainClass := Some("com.acme.Main"),
  Compile / discoveredMainClasses := Seq(),
  organization := "Acme",
  name := "AcmeFactory",
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
  ))
  .settings(commonDockerSetting)
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)



