package integration.api.invitations

import java.util.UUID

import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import api.recipes.{IngredientSummary, RecipeResp}
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo}
import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Request, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.test.{Gen, Spec, TestEnvironment, assertTrue, TestLensOptionOps, SmartAssertionOps}
import api.Authentication.AuthenticatedUser
import zio.http.Body
import zio.http.Response
import zio.http.ZClient
import db.repositories.InvitationsRepo
import com.augustnagro.magnum.magzio.{Transactor, sql}
import db.tables.storageInvitationTable

object CreateInvitationTests extends ZIOIntegrationTestSpec:
  private def endpointPath(storageId: StorageId): URL =
    URL(Path.root / "invitations" / "to" / storageId.toString)

  private def createInvitation(user: AuthenticatedUser, storageId: StorageId):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath(storageId))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Create invitation tests")(
    test("When unauthorized should get 401") {
      for
        storageId <- Gen.uuid.runHead.some
        resp <- Client.batched(post(endpointPath(storageId)))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When create invitation to owned storage should get 200 and invitation should be created"){
      for
        user <- registerUser
        storageId <- createStorage(user)

        resp <- createInvitation(user, storageId)

        invitation <- resp.body.asString
        storageIdFromDb <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${storageInvitationTable.storageId}
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitation
          """.query[StorageId].run().headOption
        ))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storageIdFromDb.is(_.some) == storageId)
    },
    test("When create invitation to membered storage should get 200 and invitation should be created"){
      for
        creator <- registerUser
        storageId <- createStorage(creator)

        user <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_
          .addMemberToStorageById(storageId, user.userId)
        )

        resp <- createInvitation(user, storageId)

        invitation <- resp.body.asString
        storageIdFromDb <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${storageInvitationTable.storageId}
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitation
          """.query[StorageId].run().headOption
        ))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(storageIdFromDb.is(_.some) == storageId)
    },
    test("When create invitation to non-existent storage should get 404 and invitation should not be created"){
      for
        user <- registerUser
        storageId <- Gen.uuid.runHead.some

        resp <- createInvitation(user, storageId)

        invitation <- resp.body.asString
        storageIdFromDb <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${storageInvitationTable.storageId}
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitation
          """.query[StorageId].run().headOption
        ))
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(storageIdFromDb.isEmpty)
    },
    test("When create invitation to neither owned nor membered storage should get 404 and invitation should not be created"){
      for
        creator <- registerUser
        storageId <- createStorage(creator)

        member <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_
          .addMemberToStorageById(storageId, member.userId)
        )

        user <- registerUser

        resp <- createInvitation(user, storageId)

        invitation <- resp.body.asString
        storageIdFromDb <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${storageInvitationTable.storageId}
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitation
          """.query[StorageId].run().headOption
        ))
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(storageIdFromDb.isEmpty)
    },
  ).provideLayer(testLayer)
