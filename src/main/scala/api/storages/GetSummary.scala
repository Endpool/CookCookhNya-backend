package api.storages

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import db.repositories.StorageMembersRepo
import db.repositories.StoragesRepo
import db.tables.DbStorage
import domain.{StorageNotFound, InternalServerError, StorageId, UserId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class GetStorageResp(
  ownerId: UserId,
  name: String,
)
object GetStorageResp:
  def fromDb(dbStorage: DbStorage): GetStorageResp =
    GetStorageResp(dbStorage.ownerId, dbStorage.name)

private type GetEnv = StoragesRepo & StorageMembersRepo

private val getSummary: ZServerEndpoint[GetEnv, Any] =
  storagesEndpoint
    .get
    .in(path[StorageId]("storageId"))
    .out(jsonBody[GetStorageResp])
    .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
    .zSecuredServerLogic(getHandler)

private def getHandler(storageId: StorageId):
  ZIO[
    AuthenticatedUser & GetEnv,
    InternalServerError | StorageNotFound,
    GetStorageResp
  ] =
  ZIO.serviceWithZIO[StoragesRepo](_
    .getById(storageId)
    .orElseFail(InternalServerError())
    .someOrFail(StorageNotFound(storageId.toString))
    .map(GetStorageResp.fromDb)
  )
