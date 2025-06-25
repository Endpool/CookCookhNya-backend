package api.storages.members

import api.AppEnv
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import api.zSecuredServerLogic
import db.tables.{usersTable, storageMembersTable, storagesTable}
import domain.{StorageError, StorageId, UserId, DbError}

import com.augustnagro.magnum.magzio.Transactor
import com.augustnagro.magnum.sql
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class UserResp(id: UserId, alias: Option[String], fullName: String)

private val getAll: ZServerEndpoint[AppEnv, Any] =
  storagesMembersEndpoint
  .get
  .out(jsonBody[Seq[UserResp]])
  .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
  .zSecuredServerLogic(getAllHandler)

private def getAllHandler(userId: UserId)(storageId: StorageId):
  ZIO[Transactor, DbError.UnexpectedDbError | StorageError.NotFound, Vector[UserResp]] = for
    members <- ZIO.serviceWithZIO[Transactor] {
      _.transact {
        sql"""
          SELECT u.${usersTable.id}, u.alias, u.${usersTable.fullName}
          FROM $usersTable u
          JOIN $storageMembersTable sm ON u.${usersTable.id} = sm.${storageMembersTable.memberId}
          WHERE sm.${storageMembersTable.storageId} = $storageId

          UNION

          SELECT u.${usersTable.id}, u.alias, u.${usersTable.fullName}
          FROM $usersTable u
          JOIN $storagesTable s on u.${usersTable.id} = s.${storagesTable.ownerId}
          WHERE s.${storagesTable.id} = $storageId
        """.query[UserResp].run()
      }
    }.mapError[DbError.UnexpectedDbError](e => DbError.UnexpectedDbError(e.getMessage))
    //         ^^^^^^^^^^^^^^^^^^^^^^^^^ <- scala is stupid

    _ <- ZIO.unless(members.map(_.id).contains(userId)) {
      ZIO.fail[StorageError.NotFound](StorageError.NotFound(storageId))
      //       ^^^^^^^^^^^^^^^^^^^^^ <- scala is stupid
    }
  yield members
