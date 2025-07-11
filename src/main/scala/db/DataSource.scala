package db

import com.augustnagro.magnum.magzio.*
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import zio.{ZLayer, Layer, ZIO, IO, System, Task, RLayer}
import zio.Scope

case class DataSourceDescription(
  jdbcUrl: String,
  username: String,
  password: String,
  driver: String
):
  def toDataSource: DataSource =
    val hikari = new HikariDataSource()
    hikari.setJdbcUrl(jdbcUrl)
    hikari.setUsername(username)
    hikari.setPassword(password)
    hikari.setDriverClassName(driver)
    hikari

object DataSourceDescription:
  def apply(
    address : String,
    dbName : String,
    username : String,
    password : String,
    driver : String = "org.postgresql.Driver"
  ): DataSourceDescription = DataSourceDescription(
    jdbcUrl = s"jdbc:postgresql://$address/$dbName",
    username,
    password,
    driver
  )

  val layerFromEnv: Layer[RuntimeException, DataSourceDescription] = ZLayer.fromZIO {
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
    yield DataSourceDescription(address, dbName, username, password, "org.postgresql.Driver")
  }

val dbLayer: RLayer[DataSource, Transactor] =
  for
    dataSource <- ZLayer.service[DataSource]
    xa <- Transactor.layer(dataSource.get)
    _  <- ZLayer.fromZIO(createTables(xa.get))
  yield xa

val dataSourceLayer: RLayer[DataSourceDescription, DataSource] =
  ZLayer.fromFunction((descr: DataSourceDescription) => descr.toDataSource)
