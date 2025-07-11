package integration.api.invitations

import api.Authentication.AuthenticatedUser
import api.recipes.{IngredientSummary, RecipeResp}
import com.augustnagro.magnum.magzio.{Transactor, sql}
import db.repositories.InvitationsRepo
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo}
import db.repositories.StoragesRepo
import db.tables.storageInvitationTable
import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.*
import zio.http.{Client, Body, Response, Path, Request, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.test.{Gen, Spec, TestEnvironment, assertTrue, TestLensOptionOps, SmartAssertionOps}

object ActivateInvitationTests extends ZIOIntegrationTestSpec:
  private def endpointPath(invitationHash: String): URL =
    URL(Path.root / "invitations" / invitationHash / "activate")

  private def activateInvitation(user: AuthenticatedUser, invitationHash: String):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath(invitationHash))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Activate invitation tests")(
    test("When unauthorized should get 401") {
      for
        invitationHash <- Gen.alphaNumericString.runHead.some
        resp <- Client.batched(post(endpointPath(invitationHash)))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When activated invalid invitation should get 400") {
      val invitationHash = "invalidinvitationhash"
      for
        user <- registerUser
        resp <- activateInvitation(user, invitationHash)
      yield assertTrue(resp.status == Status.BadRequest)
    },
    test("When activated valid invitation should get 200 and user should be added to storage members") {
      for
        creator <- registerUser

        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideUser(creator)
        )

        invitationHash <- ZIO.serviceWithZIO[InvitationsRepo](_
          .create(storageId)
          .provideUser(creator)
        )

        user <- registerUser

        resp <- activateInvitation(user, invitationHash)

        memberIds <- ZIO.serviceWithZIO[StorageMembersRepo](_
          .getAllStorageMembers(storageId)
          .provideUser(creator)
        )
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(memberIds.contains(user.userId))
    },
    test("When activated valid invitation to new storage should get 200 and invitation should be deleted from db") {
      for
        creator <- registerUser

        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideUser(creator)
        )

        invitationHash <- ZIO.serviceWithZIO[InvitationsRepo](_
          .create(storageId)
          .provideUser(creator)
        )

        user <- registerUser

        resp <- activateInvitation(user, invitationHash)

        invitationIsDeleted <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT 1
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitationHash
          """.query[Int].run().isEmpty
        ))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(invitationIsDeleted)
    },
    test("When activated valid invitation to owned storage should get 200 and invitation should NOT be deleted from db") {
      for
        creator <- registerUser

        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideUser(creator)
        )

        invitationHash <- ZIO.serviceWithZIO[InvitationsRepo](_
          .create(storageId)
          .provideUser(creator)
        )

        resp <- activateInvitation(creator, invitationHash)

        invitationExists <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT 1
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitationHash
          """.query[Int].run().headOption.isDefined
        ))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(invitationExists)
    },
    test("When activated valid invitation to membered storage should get 200 and invitation should NOT be deleted from db") {
      for
        creator <- registerUser

        storageName <- randomString
        storageId <- ZIO.serviceWithZIO[StoragesRepo](_
          .createEmpty(storageName)
          .provideUser(creator)
        )

        invitationHash <- ZIO.serviceWithZIO[InvitationsRepo](_
          .create(storageId)
          .provideUser(creator)
        )

        member <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_
          .addMemberToStorageById(storageId, member.userId)
          .provideUser(creator)
        )

        resp <- activateInvitation(member, invitationHash)

        invitationExists <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT 1
            FROM $storageInvitationTable
            WHERE ${storageInvitationTable.invitation} = $invitationHash
          """.query[Int].run().nonEmpty
        ))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(invitationExists)
    },
  ).provideLayer(testLayer)
