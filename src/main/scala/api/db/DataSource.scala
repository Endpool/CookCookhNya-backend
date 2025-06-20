package api.db

import com.augustnagro.magnum.magzio.*
import com.zaxxer.hikari.HikariDataSource
import zio.ZLayer

val dataSource = {
  val hikari = new HikariDataSource()
  hikari.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb")
  hikari.setUsername("app_user")
  hikari.setPassword("password")
  hikari.setDriverClassName("org.postgresql.Driver")
  hikari
}

val dbLayer =
  for
    xa <- Transactor.layer(dataSource)
    _ <- ZLayer(createTables(xa.get))
  yield xa