package integration.api.recipes

import api.AppEnv
import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import api.recipes.{IngredientSummary, RecipeResp}
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo}
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
      test("1 user with 1 storage") {
        for
          userId <- registerUser
          storageId <- createStorage(userId)
          ingredientIds <- createNIngredients(defaultIngredientAmount)
          extraIngredientIds <- createNIngredients(defaultIngredientAmount)
          _ <- addIngredientsToStorage(storageId, ingredientIds)

          recipeId <- createRecipe(ingredientIds)
          resp <- Client.batched(
            Request.get(s"$defaultPath/$recipeId")
              .addAuthorization(userId)
          )
          strBody <- resp.body.asString
          recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
          recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)

        yield assertTrue(resp.status == Status.Ok) &&
              assertTrue(ingredientIds == recipeRespIngredientsIds) &&
              assertTrue(recipeResp.ingredients.forall(_.inStorages == Vector(storageId)))
      },
      test("1 user with 2 storages") {
        for
          userId <- registerUser

          storageId1 <- createStorage(userId)
          storageId2 <- createStorage(userId)

          ingredientIds1 <- createNIngredients(defaultIngredientAmount)
          ingredientIds2 <- createNIngredients(defaultIngredientAmount)
          extraIngredientIds1 <- createNIngredients(defaultIngredientAmount)
          extraIngredientIds2 <- createNIngredients(defaultIngredientAmount)

          _ <- addIngredientsToStorage(storageId1, ingredientIds1 ++ extraIngredientIds1)
          _ <- addIngredientsToStorage(storageId2, ingredientIds2 ++ extraIngredientIds2)

          recipeIngredientsIds = ingredientIds1 ++ ingredientIds2
          recipeId <- createRecipe(recipeIngredientsIds)
          resp <- Client.batched(
            Request.get(s"$defaultPath/$recipeId")
              .addAuthorization(userId)
          )
          strBody <- resp.body.asString
          recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
          recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
        yield assertTrue(resp.status == Status.Ok) &&
              assertTrue(recipeIngredientsIds == recipeRespIngredientsIds) &&
              assertTrue(recipeResp.ingredients.forall(
            ingredient =>
              if ingredientIds1.contains(ingredient.id)
              then ingredient.inStorages == Vector(storageId1)
              else ingredient.inStorages == Vector(storageId2)
        ))
      },
//      test("2 users, 1 shared storage and one personal storage for every user") {
//
//      }
    ).provideLayer(testLayer)

