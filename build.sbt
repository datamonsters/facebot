lazy val http4sVersion = "0.14.4"

val publishSettings = Seq(
  publishTo := {
    isSnapshot.value match {
      case true => Some("iDecide Snapshots" at "https://nexus.flexis.ru/content/repositories/snapshots")
      case false => Some("iDecide Releases" at "https://nexus.flexis.ru/content/repositories/releases")
    }
  }
)

val commonSettings = Seq(
  organization := "co.datamonsters",
  scalaVersion := "2.11.8",
  version := "0.3.0"
)

lazy val core = project
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(
    normalizedName := "facebot-core",
    libraryDependencies ++= Seq(
      "com.github.fomkin" %% "pushka-json" % "0.7.1",
      "org.slf4j" % "slf4j-api" % "1.7.21"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val http4s = project
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(
    normalizedName := "facebot-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion
    )
  )
  .dependsOn(core)

lazy val akkahttp = project
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(
    normalizedName := "facebot-akka-http",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11"
    )

  )
  .dependsOn(core)

lazy val root = (project in file("."))
  .settings(publish := {})
  .aggregate(core, http4s, akkahttp)
