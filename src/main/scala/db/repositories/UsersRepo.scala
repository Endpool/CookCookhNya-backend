package db.repositories

import db.tables.{DbUser, usersTable}
import db.{handleDbError, handleUnfailableQuery}
import domain.UserId
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[UnexpectedDbError | DbNotRespondingError, Unit]

private final case class UsersRepoLive(xa: Transactor) extends Repo[DbUser, DbUser, UserId] with UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[UnexpectedDbError | DbNotRespondingError, Unit] =
    val user = DbUser(userId, alias, fullName)
    xa.transact {
      if existsById(user.id)
        then update(user)
        else insert(user)
    }.mapError(e => DbError.UnexpectedDbError(e.getMessage))

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))
