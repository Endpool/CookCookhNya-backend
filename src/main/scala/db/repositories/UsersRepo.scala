package db.repositories

import db.tables.Users
import domain.{UserId, DbError}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

case class UserCreationEntity(username: String)

trait UsersRepo:
  def addUserIfNotExists(username: String): IO[DbError.UnexpectedDbError, UserId]

final case class UsersRepoLive(xa: Transactor) extends Repo[UserCreationEntity, Users, UserId] with UsersRepo:
  def addUserIfNotExists(username: String): IO[DbError.UnexpectedDbError, UserId] =
    xa.transact {
      val existent: Vector[Users] = findAll(Spec[Users]
        .where(sql"${Users.table.username} = $username")
        .limit(1)
      )
      if existent.isEmpty then insertReturning(UserCreationEntity(username)).id else existent.head.id

    }.catchAll(e => ZIO.fail(DbError.UnexpectedDbError(e.getMessage)))

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))