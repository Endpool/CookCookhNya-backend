package db.repositories

import db.tables.{DbUser, usersTable}
import db.handleDbError
import domain.UserId
import domain.DbError.{UnexpectedDbError, DbNotRespondingError, FailedDbQuery}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[UnexpectedDbError | DbNotRespondingError, Unit]

final case class UsersRepoLive(xa: Transactor) extends Repo[DbUser, DbUser, UserId] with UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String):
    IO[UnexpectedDbError | DbNotRespondingError, Unit] =
    val user = DbUser(userId, alias, fullName)
    xa.transact {
      if existsById(user.id)
        then insert(user)
        else update(user)
    }.mapError {
      handleDbError(_) match
        case FailedDbQuery(msg) => UnexpectedDbError(msg)
        case error: (UnexpectedDbError | DbNotRespondingError) => error
    }

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))
