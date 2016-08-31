lazy val http4sVersion = "0.14.4"

val commonSettings = Seq(
  organization := "co.datamonsters",
  scalaVersion := "2.11.8"
)

lazy val core = project
  .settings(commonSettings:_*)
  .settings(
    normalizedName := "facebot-core",
    libraryDependencies += "com.github.fomkin" %% "pushka-json" % "0.6.2",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val http4s = project
  .settings(commonSettings:_*)
  .settings(
    normalizedName := "facebot-http4s",
    libraryDependencies ++= Seq(
      "com.github.fomkin" %% "pushka-json" % "0.7.0-SNAPSHOT",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.21"
    )
  )
  .dependsOn(core)



