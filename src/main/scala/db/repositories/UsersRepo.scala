package db.repositories

import api.Authentication.AuthenticatedUser
import db.tables.{DbUser, usersTable}
import db.{DbError, handleDbError}
import domain.UserId

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait UsersRepo:
  def saveUser(alias: Option[String], fullName: String):
    ZIO[AuthenticatedUser, DbError, Unit]

private final case class UsersRepoLive(xa: Transactor)
  extends Repo[DbUser, DbUser, UserId] with UsersRepo:
  def saveUser(alias: Option[String], fullName: String):
    ZIO[AuthenticatedUser, DbError, Unit] = for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    user = DbUser(userId, alias, fullName)
    _ <- xa.transact {
      if existsById(user.id)
        then update(user)
        else insert(user)
    }.mapError(handleDbError)
  yield ()

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))
