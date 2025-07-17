package integration.api.ingredients

import api.Authentication.AuthenticatedUser
import api.ingredients.CreateIngredientReqBody
import db.repositories.IngredientsRepo
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import zio.http.*
import zio.*
import zio.test.*

object CreateIngredientTests extends ZIOIntegrationTestSpec:
  private def endpointPath: URL =
    URL(Path.root / "ingredients")

  private def createIngredient(user: AuthenticatedUser, reqBody: CreateIngredientReqBody):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath)
        .withJsonBody(reqBody)
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Create invitation tests")(
    test("When unauthorized should get 401") {
      for
        resp <- Client.batched(post(endpointPath))
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When create ingredient should get 204 and custom ingredient should be created"){
      for
        user <- registerUser
        ingredientName <- randomString

        resp <- createIngredient(user, CreateIngredientReqBody(ingredientName))

        bodyStr <- resp.body.asString
        ingredientId <- ZIO.fromOption(bodyStr.toUUID)
          .orElse(failed(Cause.fail(s"Could not parse response ingredientId $bodyStr")))

        ingredient <- ZIO.serviceWithZIO[IngredientsRepo](_
          .get(ingredientId)
          .provideUser(user)
        )
      yield assertTrue(resp.status == Status.Created)
         && assertTrue(ingredient.is(_.some).ownerId.is(_.some) == user.userId)
         && assertTrue(ingredient.is(_.some).name == ingredientName)
    },
  ).provideLayer(testLayer)
