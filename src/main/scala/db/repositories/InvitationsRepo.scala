package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbStorageInvitation, storageInvitationTable}
import domain.{IngredientId, InternalServerError, InvalidInvitationHash, StorageAccessForbidden, StorageId, StorageNotFound, UserId}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, System, ZIO, ZLayer}

trait InvitationsRepo:
  def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError | StorageAccessForbidden, String]
  def activate(hash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, Unit]

private final case class InvitationsRepoLive(xa: Transactor)
  extends Repo[DbStorageInvitation, DbStorageInvitation, Null] with InvitationsRepo:

  override def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError, String] =
    for
      secretKey <- System.env("SECRET_KEY").someOrFail(
        new IllegalStateException("SECRET_KEY environment variable not set")
      ).mapError(e => InternalServerError(e.getMessage))

      currentTime = Instant.now().toEpochMilli
      input = s"$currentTime:$storageId:$secretKey"
      digest = MessageDigest.getInstance("SHA-256")
      hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8))
      invitationHash = hashBytes.map("%02x".format(_)).mkString
      _ <- xa.transact {
        insert(DbStorageInvitation(storageId, invitationHash))
      }.mapError(handleDbError)
    yield invitationHash

  override def activate(invitationHash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, Unit] =
    for
      dbInvitation <- xa.transact {
        findAll(Spec[DbStorageInvitation]
          .where(sql"${storageInvitationTable.invitation} = $invitationHash")
        ).headOption
      }.mapError(handleDbError).someOrFail(InvalidInvitationHash(invitationHash))
      isMemberOrOwner <- ZIO.serviceWithZIO[StorageMembersRepo](_.checkForMembership(dbInvitation.storageId))
      _ <- ZIO.unless(isMemberOrOwner) {
        for
          _ <- xa.transact(delete(dbInvitation)).mapError(handleDbError)
          userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
          _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(dbInvitation.storageId, userId))
        yield ()
      }
    yield ()

object InvitationsRepo:
  val layer = ZLayer.fromFunction(InvitationsRepoLive(_))
