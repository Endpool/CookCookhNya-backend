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
        invitationHash <- Gen.string.runHead.someOrElse("aboba")
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
  ).provideLayer(testLayer)
