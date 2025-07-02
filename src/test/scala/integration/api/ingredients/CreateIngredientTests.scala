package integration.api.ingredients

import api.ingredients.CreateIngredientReqBody
import db.dbLayer
import db.repositories.IngredientsRepo
import db.repositories.StoragesRepo
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import io.circe.generic.auto.*
import zio.http.{Client, Status}
import zio.{Scope, ZIO}
import zio.test.Assertion.*
import zio.test.{SmartAssertMacros, SmartAssertionOps, Spec, TestEnvironment, TestLensOptionOps, assert, assertTrue}

object CreateIngredientTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
      test("Should result in 200") {
        for
          resp <- Client.batched(
            post("ingredients")
              .withJsonBody(CreateStorageReqBody("ingredient"))
          )
        yield assertTrue(resp.status == Status.Ok)
      },
    ).provideLayer(testLayer)
