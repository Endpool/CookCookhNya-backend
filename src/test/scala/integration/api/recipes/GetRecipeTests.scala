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
          Request.get(s"$defaultPath/$getRandomUUID")
        ).map(resp => assertTrue(resp.status == Status.Unauthorized))
      },
      test("When asked for non-existent recipe, 404 should be returned"){
        for
          user <- registerUser
          resp <- Client.batched(
            Request.get(defaultPath / getRandomUUID.toString)
              .addAuthorization(user)
          )
        yield assertTrue(resp.status == Status.NotFound)
      },
      test("1 user with 1 storage") {
        for
          user <- registerUser
          storageId <- createStorage(user)
          ingredientIds       <- createNIngredients(defaultIngredientAmount)
          _extraIngredientIds <- createNIngredients(defaultIngredientAmount)
          _ <- addIngredientsToStorage(storageId, ingredientIds)

          recipeId <- createRecipe(ingredientIds)

          resp <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(user)
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
          user <- registerUser

          storage1Id <- createStorage(user)
          storage2Id <- createStorage(user)

          storage1UsedIngredientIds <- createNIngredients(defaultIngredientAmount)
          storage2UsedIngredientIds <- createNIngredients(defaultIngredientAmount)
          recipeIngredientsIds = storage1UsedIngredientIds
                              ++ storage2UsedIngredientIds

          recipeId <- createRecipe(recipeIngredientsIds)

          _ <- addIngredientsToStorage(storage1Id, storage1UsedIngredientIds)
          _ <- addIngredientsToStorage(storage2Id, storage2UsedIngredientIds)

          // create some extra ingredients that are not used in the recipe
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(storage1Id, _))
          _ <- createNIngredients(defaultIngredientAmount)
            .flatMap(addIngredientsToStorage(storage2Id, _))

          resp <- Client.batched(
            Request.get(defaultPath / recipeId.toString)
              .addAuthorization(user)
          )

          strBody <- resp.body.asString
          recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
          recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
        yield assertTrue(resp.status == Status.Ok)
           && assertTrue(recipeRespIngredientsIds hasSameElementsAs recipeIngredientsIds)
           && assertTrue(recipeResp.ingredients.forall(ingredient =>
                ingredient.inStorages == (
                  if storage1UsedIngredientIds.contains(ingredient.id)
                    then Vector(storage1Id)
                  else if storage2UsedIngredientIds.contains(ingredient.id)
                    then Vector(storage2Id)
                  else Vector.empty
                )
              ))
      },
      test("2 users, 1 shared storage and 1 personal storage for every user") {
        for
          user1 <- registerUser
          user2 <- registerUser

          user1StorageId  <- createStorage(user1)
          sharedStorageId <- createStorage(user1)
          _ <- ZIO.serviceWithZIO[StorageMembersRepo](
            _.addMemberToStorageById(sharedStorageId, user2.userId)
          )
          user2StorageId  <- createStorage(user2)

          temp <- for
            commonIngredientIds           <- createNIngredients(defaultIngredientAmount)
            user1OnlyStorageIngredientIds <- createNIngredients(defaultIngredientAmount)
            user2OnlyStorageIngredientIds <- createNIngredients(defaultIngredientAmount)
          yield (user1OnlyStorageIngredientIds ++ commonIngredientIds,
                 user2OnlyStorageIngredientIds ++ commonIngredientIds)
          (user1StorageIngredientIds, user2StorageIngredientIds) = temp
          _ <- addIngredientsToStorage(user1StorageId, user1StorageIngredientIds)
          _ <- addIngredientsToStorage(user2StorageId, user2StorageIngredientIds)

          sharedStorageIngredientIds <- createNIngredients(defaultIngredientAmount)
          _ <- addIngredientsToStorage(sharedStorageId, sharedStorageIngredientIds)

          recipeIngredientsIds =
            (  user1StorageIngredientIds
            ++ user2StorageIngredientIds
            ++ sharedStorageIngredientIds
            ).distinct
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
                  .addAuthorization(user1)
              )

              strBody <- resp.body.asString
              recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
              recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
            yield assertTrue(resp.status == Status.Ok)
               && assertTrue(recipeRespIngredientsIds hasSameElementsAs recipeIngredientsIds)
               && assertTrue(recipeResp.ingredients.forall( ingredient =>
                   ingredient.inStorages == (
                     if (user1StorageIngredientIds.contains(ingredient.id))
                       Vector(user1StorageId)
                     else if (sharedStorageIngredientIds.contains(ingredient.id))
                       Vector(sharedStorageId)
                     else
                       Vector.empty
                     )
                   ))
          }

          // case 2: sending request as a 2nd user
          assertCase2 <- {
            for
              resp <- Client.batched(
                Request.get(defaultPath / recipeId.toString)
                  .addAuthorization(user2)
              )

              strBody <- resp.body.asString
              recipeResp <- ZIO.fromEither(decode[RecipeResp](strBody))
              recipeRespIngredientsIds = recipeResp.ingredients.map(_.id)
           yield assertTrue(resp.status == Status.Ok)
              && assertTrue(recipeRespIngredientsIds.hasSameElementsAs(recipeIngredientsIds))
              && assertTrue(recipeResp.ingredients.forall( ingredient =>
                  ingredient.inStorages == (
                    if (user2StorageIngredientIds.contains(ingredient.id))
                      Vector(user2StorageId)
                    else if (sharedStorageIngredientIds.contains(ingredient.id))
                      Vector(sharedStorageId)
                    else
                      Vector.empty
                    )
                 ))
          }
        yield assertCase1 && assertCase2
      }
    ).provideLayer(testLayer)

