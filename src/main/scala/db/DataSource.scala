package db

import com.augustnagro.magnum.magzio.*
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import zio.{ZLayer, IO, System}

val dbLayer: ZLayer[Any, Throwable, Transactor] =
  for
    ds <- ZLayer.fromZIO(dataSource)
    xa <- Transactor.layer(ds.get)
    _  <- ZLayer(createTables(xa.get))
  yield xa

val dataSource: IO[RuntimeException, DataSource] =
  for
    address  <- System.env("DB_ADDRESS").someOrFail(
      new IllegalStateException("DB_ADDRESS environment variable not set")
    )
    dbName   <- System.env("DB_NAME").someOrFail(
      new IllegalStateException("DB_NAME environment variable not set")
    )
    username <- System.env("DB_USER").someOrFail(
      new IllegalStateException("DB_USER environment variable not set")
    )
    password <- System.env("DB_PASSWORD").someOrFail(
      new IllegalStateException("DB_PASSWORD environment variable not set")
    )
  yield mkDataSource(s"jdbc:postgresql://$address/$dbName", username, password)

private def mkDataSource(url: String, username: String, password: String) =
  val hikari = new HikariDataSource()
  hikari.setJdbcUrl(url)
  hikari.setUsername(username)
  hikari.setPassword(password)
  hikari.setDriverClassName("org.postgresql.Driver")
  hikari
