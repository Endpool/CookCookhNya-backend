package db.repositories

import db.tables.Users
import domain.{UserId, DbError}

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String): IO[DbError.UnexpectedDbError, Unit]

final case class UsersRepoLive(xa: Transactor) extends Repo[Users, Users, UserId] with UsersRepo:
  def saveUser(userId: UserId, alias: Option[String], fullName: String): IO[DbError.UnexpectedDbError, Unit] =
    val user = Users(userId, alias, fullName)
    xa.transact {
      val existent: Vector[Users] = findAll(Spec[Users]
        .where(sql"${Users.table.id} = ${user.id}")
        .limit(1)
      )
      if existent.isEmpty then insert(user) else update(user)

    }.catchAll(e => ZIO.fail(DbError.UnexpectedDbError(e.getMessage)))

object UsersRepo:
  val layer = ZLayer.fromFunction(UsersRepoLive(_))