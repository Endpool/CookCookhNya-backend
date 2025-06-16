ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"
val zioHttpVersion = "3.2.0"
val zioVersion = "2.1.19"
val sttpVersion = "4.0.7"
val tapirVersion = "1.11.33"
val ironVersion = "3.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "CookCookHny-backend",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,

      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,

      "com.augustnagro" %% "magnumzio" % "2.0.0-M1",
      "com.zaxxer"    % "HikariCP"    % "6.3.0", // connection pool
      "org.postgresql" % "postgresql" % "42.7.7",
    )
  )
