package api.storages.members

import api.{
  AppEnv,
  handleFailedSqlQuery,
  toUserNotFound,
  toStorageNotFound
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant,
  userNotFoundVariant}
import api.zSecuredServerLogic
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.StorageMembersRepo
import domain.{StorageError, InternalServerError, UserError, UserId, StorageId}

import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import sttp.model.StatusCode
import zio.ZIO

private val remove: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .delete
  .in(path[UserId]("memberId"))
  .out(statusCode(StatusCode.NoContent))
  .errorOut(oneOf(serverErrorVariant, userNotFoundVariant, storageNotFoundVariant))
  .zSecuredServerLogic(removeHandler)

private def removeHandler(userId: UserId)(storageId: StorageId, memberId: UserId):
  ZIO[StorageMembersRepo, InternalServerError | UserError.NotFound | StorageError.NotFound, Unit] =
  ZIO.serviceWithZIO[StorageMembersRepo] {
    _.removeMemberFromStorageById(storageId, memberId)
  }.catchAll {
    case DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: FailedDbQuery => 
      for {
        keyName <- handleFailedSqlQuery(e)
        _ <- toUserNotFound(keyName, userId)
        _ <- toStorageNotFound(keyName, storageId)
      } yield ()
  }
