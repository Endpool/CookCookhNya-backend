package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.DbUser
import db.DbError
import db.QuillConfig.provideDS

import io.getquill.*
import javax.sql.DataSource
import zio.{ZIO, ZLayer}

trait UsersRepo:
  def saveUser(alias: Option[String], fullName: String):
    ZIO[AuthenticatedUser, DbError, Unit]

private final case class UsersRepoLive(dataSource: DataSource) extends UsersRepo:
  import db.QuillConfig.ctx.*

  private given DataSource = dataSource

  def saveUser(alias: Option[String], fullName: String):
    ZIO[AuthenticatedUser, DbError, Unit] = for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    user = DbUser(userId, alias, fullName)
    _ <- run(
      query[DbUser]
        .insertValue(lift(user))
        .onConflictUpdate(_.id)(
          (t, e) => t.alias    -> e.alias,
          (t, e) => t.fullName -> e.fullName,
        )
    ).provideDS
  yield ()

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))
