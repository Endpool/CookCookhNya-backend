enablePlugins(JavaAppPackaging)

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

val zioHttpVersion = "3.2.0"
val zioVersion = "2.1.19"
val sttpVersion = "4.0.7"
val tapirVersion = "1.11.33"
val ironVersion = "3.0.0"
val circeVersion = "0.14.14"

lazy val root = (project in file("."))
  .settings(
    name := "CookCookHnya-backend",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,

      // tapir
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,

      // db
      "com.augustnagro" %% "magnumzio" % "2.0.0-M1",
      "com.zaxxer" % "HikariCP" % "6.3.0", // connection pool
      "org.postgresql" % "postgresql" % "42.7.7",

      "io.circe" %% "circe-generic" % circeVersion,

      "ch.qos.logback" % "logback-classic" % "1.4.14",

      "me.xdrop" % "fuzzywuzzy" % "1.4.0",

      "commons-codec" % "commons-codec" % "1.16.0",

      // tests
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-http-testkit" % "3.3.3" % Test,

      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.4" % Test,
      "org.testcontainers" % "postgresql" % "1.20.2" % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
