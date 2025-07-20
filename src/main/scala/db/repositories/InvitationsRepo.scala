package db.repositories

import api.Authentication.AuthenticatedUser
import db.{DbError, handleDbError}
import db.tables.{DbStorage, DbStorageInvitation, storageInvitationTable}
import domain.{InternalServerError, InvalidInvitationHash, StorageAccessForbidden, StorageId}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import io.getquill.*

import javax.sql.DataSource
import com.augustnagro.magnum.magzio.*
import zio.{Clock, Layer, System, URLayer, ZIO, ZLayer}

trait InvitationsRepo:
  def create(storageId: StorageId):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InternalServerError | StorageAccessForbidden, String]
  def activate(hash: String):
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, (StorageId, String)]

private final case class InvitationsRepoLive(xa: Transactor, dataSource: DataSource,  secretKey: InvitationsSecretKey)
  extends Repo[DbStorageInvitation, DbStorageInvitation, Null] with InvitationsRepo:
  private given DataSource = dataSource
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import InvitationQueries.*

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
    ZIO[AuthenticatedUser & StorageMembersRepo, DbError | InvalidInvitationHash, (StorageId, String)] =
    for
      dbInvitationWithName <- run(getInvitationWithStorageNameByHashQ(lift(invitationHash)))
        .provideDS
        .map(_.headOption)
        .someOrFail(InvalidInvitationHash(invitationHash))
      (dbInvitation, storageName) = dbInvitationWithName
      isMemberOrOwner <- ZIO.serviceWithZIO[StorageMembersRepo](_.checkForMembership(dbInvitation.storageId))
      _ <- ZIO.unless(isMemberOrOwner) {
        for
          _ <- xa.transact(delete(dbInvitation)).mapError(handleDbError)
          userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
          _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(dbInvitation.storageId, userId))
        yield ()
      }
    yield (dbInvitation.storageId, storageName)

object InvitationQueries:
  inline def getInvitationWithStorageNameByHashQ(inline invitationHash: String): Query[(DbStorageInvitation, String)] =
    query[DbStorageInvitation]
      .filter(_.invitation == invitationHash)
      .join(query[DbStorage])
      .on(_.storageId == _.id)
      .map((i, s) => (i, s.name))

object InvitationsRepo:
  val layer: URLayer[Transactor & DataSource & InvitationsSecretKey, InvitationsRepoLive] =
    ZLayer.fromZIO(
      for
        xa <- ZIO.service[Transactor]
        dataSource <- ZIO.service[DataSource]
        secretKey <- ZIO.service[InvitationsSecretKey]
      yield InvitationsRepoLive(xa, dataSource, secretKey)
    )

final case class InvitationsSecretKey(value: String)

object InvitationsSecretKey:
  val layerFromEnv: Layer[SecurityException | IllegalStateException, InvitationsSecretKey] =
    ZLayer.fromZIO(
      System.env("INVITATIONS_SECRET_KEY").someOrFail[String, SecurityException | IllegalStateException](
        new IllegalStateException("INVITATIONS_SECRET_KEY environment variable not set")
      ).map(InvitationsSecretKey(_))
  )

