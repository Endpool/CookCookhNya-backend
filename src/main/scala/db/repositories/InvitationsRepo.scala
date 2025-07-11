package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbStorageInvitation, storageInvitationTable}
import domain.{IngredientId, InternalServerError, InvalidInvitationHash, StorageAccessForbidden, StorageId, StorageNotFound, UserId}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.augustnagro.magnum.magzio.*
import zio.{IO, URLayer, Layer, System, ZIO, ZLayer, Clock}

trait InvitationsRepo:
  def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError | StorageAccessForbidden, String]
  def activate(hash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, Unit]

private final case class InvitationsRepoLive(xa: Transactor, secretKey: InvitationsSecretKey)
  extends Repo[DbStorageInvitation, DbStorageInvitation, Null] with InvitationsRepo:

  override def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError, String] =
    for
      currentTime <- Clock.currentTime(TimeUnit.NANOSECONDS)
      input = s"$currentTime:$storageId:${secretKey.value}"
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
  val layer: URLayer[Transactor & InvitationsSecretKey, InvitationsRepoLive] =
    ZLayer.fromZIO(
      for
        xa <- ZIO.service[Transactor]
        secretKey <- ZIO.service[InvitationsSecretKey]
      yield InvitationsRepoLive(xa, secretKey)
    )

final case class InvitationsSecretKey(value: String)

object InvitationsSecretKey:
  val layerFromEnv: Layer[SecurityException | IllegalStateException, InvitationsSecretKey] =
    ZLayer.fromZIO(
      System.env("INVITATIONS_SECRET_KEY").someOrFail[String, SecurityException | IllegalStateException](
        new IllegalStateException("INVITATIONS_SECRET_KEY environment variable not set")
      ).map(InvitationsSecretKey(_))
  )

