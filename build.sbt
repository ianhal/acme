
ThisBuild / version := "1.0.0"
ThisBuild / scalaVersion := "2.13.8"

val typeSafeConfigVersion = "1.4.2"
val typeSafeScalaLoggingVersion = "3.9.5"
val catsEffectVersion = "3.3.11"
val logbackVersion = "1.2.11"
val scalaTestVersion = "3.2.12"

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
    name := "AcmeFactory",
    organization := "Acme",
    idePackagePrefix := Some("com.acme"),
    Compile / mainClass := Some("com.acme.Main"),
    Compile / discoveredMainClasses := Seq())
  .settings(
    libraryDependencies ++= Seq(
    "com.typesafe" % "config" % typeSafeConfigVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % typeSafeScalaLoggingVersion,
    "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
    "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,

    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  ))
  .settings(commonDockerSetting)
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
