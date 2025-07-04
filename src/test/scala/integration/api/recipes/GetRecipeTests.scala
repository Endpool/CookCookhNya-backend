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
          ingredientIds       <- createNIngredients(defaultIngredientAmount)
          _extraIngredientIds <- createNIngredients(defaultIngredientAmount)
          _ <- addIngredientsToStorage(storageId, ingredientIds)

          recipeId <- createRecipe(ingredientIds)

          resp <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(userId)
          )

          strBody <- resp.body.asString
          recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
          recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(ingredientIds hasSameElementsAs recipeRespIngredientsIds)
           && assertTrue(recipeResp.ingredients.forall(_.inStorages == Vector(storageId)))
      },
      test("1 user with 2 storages") {
        for
          userId <- registerUser

          storage1Id <- createStorage(userId)
          storage2Id <- createStorage(userId)

          usedStorage1IngredientIds <- createNIngredients(defaultIngredientAmount)
          storage2UsedIngredientIds <- createNIngredients(defaultIngredientAmount)
          recipeIngredientsIds = usedStorage1IngredientIds
                              ++ storage2UsedIngredientIds

          _ <- addIngredientsToStorage(storage1Id, usedStorage1IngredientIds)
          _ <- addIngredientsToStorage(storage2Id, storage2UsedIngredientIds)
          recipeId <- createRecipe(recipeIngredientsIds)

          // create some extra ingredients that are not used in the recipe
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(storage1Id, _))
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(storage2Id, _))

          resp <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(userId)
          )

          strBody <- resp.body.asString
          recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
          recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(recipeRespIngredientsIds hasSameElementsAs recipeIngredientsIds)
           && assertTrue(recipeResp.ingredients.forall(ingredient =>
                ingredient.inStorages == (
                  if usedStorage1IngredientIds.contains(ingredient.id)
                    then Vector(storage1Id)
                  else if storage2UsedIngredientIds.contains(ingredient.id)
                    then Vector(storage2Id)
                  else Vector.empty
                )
              ))
      },
      test("2 users, 1 shared storage and 1 personal storage for every user") {
        for
          userId1 <- registerUser
          userId2 <- registerUser

          user1StorageId  <- createStorage(userId1)
          sharedStorageId <- createStorage(userId1)
          _ <- ZIO.serviceWithZIO[StorageMembersRepo](_.addMemberToStorageById(sharedStorageId, userId2))
          user2StorageId  <- createStorage(userId2)

          temp <- for
            commonIngredientIds <- createNIngredients(defaultIngredientAmount)
            u1SIngredientIds    <- createNIngredients(defaultIngredientAmount)
            u2SIngredientIds    <- createNIngredients(defaultIngredientAmount)
          yield (u1SIngredientIds ++ commonIngredientIds,
                 u2SIngredientIds ++ commonIngredientIds)
          (user1StorageIngredientIds, user2StorageIngredientIds) = temp
          _ <- addIngredientsToStorage(user1StorageId, user1StorageIngredientIds)
          _ <- addIngredientsToStorage(user2StorageId, user2StorageIngredientIds)

          sharedStorageIngredientIds <- createNIngredients(defaultIngredientAmount)
          _ <- addIngredientsToStorage(sharedStorageId, sharedStorageIngredientIds)

          recipeIngredientsIds
            =  user1StorageIngredientIds
            ++ user2StorageIngredientIds
            ++ sharedStorageIngredientIds
          recipeId <- createRecipe(recipeIngredientsIds)

          // create some extra ingredients that are not used in the recipe
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(user1StorageId, _))
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(user2StorageId, _))
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(sharedStorageId, _))

          // case 1: sending request as a 1st user
          assertCase1 <- {
            for
              resp <- Client.batched(
                Request.get(defaultPath / recipeId.toString)
                  .addAuthorization(userId1)
              )

              strBody <- resp.body.asString
              recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
              recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
            yield assertTrue(resp.status == Status.Ok)
               && assertTrue(recipeRespIngredientsIds hasSameElementsAs recipeIngredientsIds)
               && assertTrue(recipeResp.ingredients.forall(
                              ingredient =>
                                if (user1StorageIngredientIds.contains(ingredient.id))
                                  ingredient.inStorages == Vector(user1StorageId)
                                else if (sharedStorageIngredientIds.contains(ingredient.id))
                                  ingredient.inStorages == Vector(sharedStorageId)
                                else true
                            ))
          }

          // case 2: sending request as a 2nd user
          assertCase2 <- {
            for
              resp2 <- Client.batched(
                Request.get(defaultPath / recipeId.toString)
                  .addAuthorization(userId2)
              )

              strBody2 <- resp2.body.asString
              recipeResp2 <- ZIO.fromEither(decode[RecipeResp](strBody2))
              recipeRespIngredientsIds2 = recipeResp2.ingredients.map(_.id)
           yield assertTrue(resp2.status == Status.Ok)
              && assertTrue(recipeRespIngredientsIds2.hasSameElementsAs(recipeIngredientsIds))
              && assertTrue(recipeResp2.ingredients.forall(
                   ingredient =>
                     if (user2StorageIngredientIds.contains(ingredient.id))
                       ingredient.inStorages == Vector(user2StorageId)
                     else if (sharedStorageIngredientIds.contains(ingredient.id))
                       ingredient.inStorages == Vector(sharedStorageId)
                     else ingredient.inStorages.isEmpty
                 ))
          }
        yield assertCase1 && assertCase2
      }
    ).provideLayer(testLayer)

