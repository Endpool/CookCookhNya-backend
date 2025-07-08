package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbStorageInvitation, storageInvitationTable}
import domain.{IngredientId, InternalServerError, InvalidInvitationHash, StorageAccessForbidden, StorageId, UserId}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, System, UIO, ZIO, ZLayer}

trait InvitationsRepo:
  def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError | StorageAccessForbidden, String]
  def activate(hash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, Unit]
  def getStorageIdByHash(invitationHash: String):
    ZIO[AuthenticatedUser, DbError, Option[StorageId]]

private final case class InvitationsRepoLive(xa: Transactor)
  extends Repo[DbStorageInvitation, DbStorageInvitation, StorageId & String] with InvitationsRepo:

  def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError | StorageAccessForbidden, String] =
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      isMemberOrOwner <- ZIO.serviceWithZIO[StorageMembersRepo](_.checkForMembership(userId, storageId))
      _ <- ZIO.fail(StorageAccessForbidden(storageId.toString)).unless(isMemberOrOwner)

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
    
  def activate(invitationHash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, Unit] =
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      row: DbStorageInvitation <- xa.transact {
        val spec = Spec[DbStorageInvitation]
          .where(sql"${storageInvitationTable.invitation} = $invitationHash")
        findAll(spec).headOption 
      }.mapError(handleDbError).someOrFail(InvalidInvitationHash(invitationHash))
      isMemberOrOwner <- ZIO.serviceWithZIO[StorageMembersRepo](_.checkForMembership(userId, row.storageId))
      _ <- ZIO.succeed(()).when(isMemberOrOwner)
      _ <- xa.transact {
        delete(row)
      }.mapError(handleDbError)
      _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(row.storageId, userId))
    yield ()