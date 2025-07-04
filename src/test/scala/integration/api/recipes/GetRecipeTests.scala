package integration.api.recipes

import domain.{IngredientId, StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec
import api.recipes.{IngredientSummary, RecipeResp}
import db.repositories.{StorageIngredientsRepo, StorageMembersRepo}
import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Request, Status, URL}
import zio.{Scope, ZIO}
import zio.test.{Spec, TestEnvironment, assertTrue}

object GetRecipeTests extends ZIOIntegrationTestSpec:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    val defaultPath = URL(Path.root / "recipes")
    val defaultIngredientAmount = 3

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
            Request.get(defaultPath / "1")
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
            Request.get(defaultPath / recipeId.toString)
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
            Request.get(defaultPath / recipeId.toString)
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
      test("2 users, 1 shared storage and 1 personal storage for every user") {
        for
          userId1 <- registerUser
          userId2 <- registerUser

          storageId1 <- createStorage(userId1)
          storageId2 <- createStorage(userId2)
          sharedStorageId <- createStorage(userId1)

          ingredientIds1 <- createNIngredients(defaultIngredientAmount)
          ingredientIds2 <- createNIngredients(defaultIngredientAmount)
          sharedIngredientIds <- createNIngredients(defaultIngredientAmount)
          extraIngredientIds1 <- createNIngredients(defaultIngredientAmount)
          extraIngredientIds2 <- createNIngredients(defaultIngredientAmount)
          extraSharedIngredients <- createNIngredients(defaultIngredientAmount) // used in the shared repo
          commonIngredients <- createNIngredients(defaultIngredientAmount) // used in both non-shared repos

          _ <- addIngredientsToStorage(storageId1, ingredientIds1 ++ extraIngredientIds1 ++ commonIngredients)
          _ <- addIngredientsToStorage(storageId2, ingredientIds2 ++ extraIngredientIds2 ++ commonIngredients)
          _ <- addIngredientsToStorage(sharedStorageId, sharedIngredientIds ++ extraSharedIngredients)

          recipeIngredientsIds = ingredientIds1 ++ ingredientIds2 ++ sharedIngredientIds ++ commonIngredients
          recipeId <- createRecipe(recipeIngredientsIds)

          _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(sharedStorageId, userId2))
          // case 1: sending request as a 1st user
          resp1 <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(userId1)
          )
          strBody1 <- resp1.body.asString
          recipeResp1 <- ZIO.fromEither(decode[RecipeResp](strBody1))
          recipeRespIngredientsIds1 = recipeResp1.ingredients.map(_.id)

          assertions1 = assertTrue(resp1.status == Status.Ok) &&
                        assertTrue(recipeRespIngredientsIds1.hasSameElementsAs(recipeIngredientsIds)) &&
                        assertTrue(recipeResp1.ingredients.forall(
                          ingredient =>
                            if ((ingredientIds1 ++ commonIngredients).contains(ingredient.id))
                              ingredient.inStorages.hasSameElementsAs(Vector(storageId1))
                            else if (sharedIngredientIds.contains(ingredient.id))
                              ingredient.inStorages.hasSameElementsAs(Vector(sharedStorageId))
                            else true
                        ))
          // case 2: sending request as a 2nd user
          resp2 <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(userId2)
          )
          strBody2 <- resp2.body.asString
          recipeResp2 <- ZIO.fromEither(decode[RecipeResp](strBody2))
          recipeRespIngredientsIds2 = recipeResp2.ingredients.map(_.id)

          assertions2 = assertTrue(resp2.status == Status.Ok) &&
                        assertTrue(recipeRespIngredientsIds2.hasSameElementsAs(recipeIngredientsIds)) &&
                        assertTrue(recipeResp2.ingredients.forall(
                          ingredient =>
                            if ((ingredientIds2 ++ commonIngredients).contains(ingredient.id))
                              ingredient.inStorages.hasSameElementsAs(Vector(storageId2))
                            else if (sharedIngredientIds.contains(ingredient.id))
                              ingredient.inStorages.hasSameElementsAs(Vector(sharedStorageId))
                            else ingredient.inStorages.isEmpty
                        ))

        yield assertions1 && assertions2

      }
    ).provideLayer(testLayer)

