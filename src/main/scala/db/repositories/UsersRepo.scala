package db.repositories

import db.tables.{DbUser, usersTable}
import db.{DbError, handleDbError}
import domain.UserId

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[DbError, Unit]

private final case class UsersRepoLive(xa: Transactor)
  extends Repo[DbUser, DbUser, UserId] with UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[DbError, Unit] =
    val user = DbUser(userId, alias, fullName)
    xa.transact {
      if existsById(user.id)
        then update(user)
        else insert(user)
    }.mapError(handleDbError)

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))
