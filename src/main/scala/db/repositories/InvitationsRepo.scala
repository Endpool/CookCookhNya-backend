package db.repositories

import db.{DbError, handleDbError}
import db.tables.DbStorageInvitation
import domain.{IngredientId, StorageId, UserId}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer, System}

trait InvitationsRepo:
  def create(storageId: StorageId): ZIO[UserId, DbError, String]
  def activate(hash: String): ZIO[UserId, DbError, Unit]

private final case class InvitationsRepoLive(xa: Transactor)
  extends Repo[DbStorageInvitation, DbStorageInvitation, StorageId & String] with InvitationsRepo:

  def create(storageId: StorageId): ZIO[UserId, DbError | RuntimeException, String] =
    for
      userId <- ZIO.service[UserId]

      secretKey <- System.env("SECRET_KEY").someOrFail(
        new IllegalStateException("SECRET_KEY environment variable not set")
      )
      currentTime = Instant.now().toEpochMilli
      input = s"$currentTime:$storageId:$secretKey"
      digest = MessageDigest.getInstance("SHA-256")
      hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8))
      invitationHash = hashBytes.map("%02x".format(_)).mkString
      _ <- xa.transact {
        insert(DbStorageInvitation(storageId, invitationHash))
      }
    yield hashHash