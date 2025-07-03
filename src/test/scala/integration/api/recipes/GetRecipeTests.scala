package integration.api.recipes

import api.AppEnv
import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import api.recipes.{IngredientSummary, RecipeResp}
import db.repositories.StorageIngredientsRepo
import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Request, Status}
import zio.{Scope, ZIO}
import zio.test.{SmartAssertionOps, Spec, TestEnvironment, TestLensOptionOps, assertTrue}

object GetRecipeTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    val defaultPath = "recipes/"
    val defaultIngredientAmount = 5

    suite("Get recipe (detailed) tests")(
      test("When unauthorized should get 401") {
        Client.batched(
          Request.get(s"$defaultPath/1")
        ).map(resp => assertTrue(resp.status == Status.Unauthorized))
      },
      test("When asked for non-existent recipe, 404 should be returned"){
        for
          userId <- registerUser
          resp <- Client.batched(
            Request.get(s"$defaultPath/1")
              .addAuthorization(userId)
          )
        yield assertTrue(resp.status == Status.NotFound)
      },

    ).provideLayer(testLayer)

