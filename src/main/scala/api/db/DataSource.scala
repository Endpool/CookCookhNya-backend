package api.db

import com.augustnagro.magnum.magzio.*
import com.zaxxer.hikari.HikariDataSource
import zio.ZLayer

val dataSource = {
  val hikari = new HikariDataSource()
  hikari.setJdbcUrl(sys.env("DB_URL"))
  hikari.setUsername(sys.env("DB_USER"))
  hikari.setPassword(sys.env("DB_PASSWORD"))
  hikari.setDriverClassName("org.postgresql.Driver")
  hikari
}

val dbLayer =
  for
    xa <- Transactor.layer(dataSource)
    _ <- ZLayer(createTables(xa.get))
  yield xa