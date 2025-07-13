package integration.api.recipes

import api.Authentication.AuthenticatedUser
import api.recipes.SuggestedRecipesResp
import domain.{StorageId}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import io.circe.generic.auto.*
import io.circe.parser.decode
import zio.*
import zio.http.Request.get
import zio.http.{Response, Client, Path, Request, Status, URL}
import zio.test.*
import db.repositories.StorageMembersRepo

object GetSuggestedRecipesTests extends ZIOIntegrationTestSpec:
  private val endpointPath = URL(Path.root / "recipes" / "suggested")

  private def getSuggestedRecipes(user: AuthenticatedUser, storageIds: Seq[StorageId]):
    RIO[Client, Response] =
    Client.batched(
      get(endpointPath)
        .addQueryParams("storage-id", Chunk.fromIterable(storageIds.map(_.toString)))
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Get suggested recipes tests")(
    test("When unauthorized should get 401") {
      for
        resp <- Client.batched(
          Request.get(endpointPath)
        )
      yield assertTrue(resp.status == Status.Unauthorized)
    },
    test("When authorized should get 200") {
      for
        user <- registerUser
        resp <- getSuggestedRecipes(user, Seq.empty)
      yield assertTrue(resp.status == Status.Ok)
    },
    test("When storages list is empty, for all recipes there should be 0 available ingredients") {
      for
        user <- registerUser

        n <- Gen.int(2, 10).runHead.some
        recipeIds <- ZIO.collectAll(
          (1 to n).map(i =>
            createNIngredients(i).flatMap(
              createRecipe(user, _)
            )
          )
        )

        resp <- getSuggestedRecipes(user, Seq.empty)

        bodyStr <- resp.body.asString
        suggestedRecipes <- ZIO.fromEither(decode[SuggestedRecipesResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(suggestedRecipes.recipes.map(_.id).forall(recipeIds.contains))
         && assertTrue(suggestedRecipes.recipes.map(_.available).forall(_ == 0))
    },
    test("When querying with owned storages, availabilities should be correct") {
      for
        user <- registerUser

        storage1Id <- createStorage(user)
        storage2Id <- createStorage(user)

        minStorageIngredientsAmount = 4

        tmp <- ZIO.foreach(Seq(storage1Id, storage2Id))(storageId => for
          n <- Gen.int(3, 10).runHead.some
          ingredientIds <- createNIngredients(n)
          _ <- addIngredientsToStorage(storage1Id, ingredientIds)
        yield ingredientIds)
        Seq(storage1IngredientIds, storage2IngredientIds) = tmp

        n <- Gen.int(2, 10).runHead.some
        otherIngredientIds <- createNIngredients(n)

        recipe1Storage1Availability = minStorageIngredientsAmount
        recipe1Storage2Availability = minStorageIngredientsAmount - 1
        recipe1IngredientIds
          =  storage1IngredientIds.take(recipe1Storage1Availability)
          ++ storage2IngredientIds.take(recipe1Storage2Availability)
          ++ otherIngredientIds
        recipe1Id <- createRecipe(user, recipe1IngredientIds)

        recipe2Storage1Availability = minStorageIngredientsAmount - 2
        recipe2Storage2Availability = minStorageIngredientsAmount
        recipe2IngredientIds
          =  storage1IngredientIds.takeRight(recipe2Storage1Availability)
          ++ storage2IngredientIds.takeRight(recipe2Storage2Availability)
          ++ otherIngredientIds
        recipe2Id <- createRecipe(user, recipe2IngredientIds)

        resp <- getSuggestedRecipes(user, Seq(storage1Id, storage2Id))

        bodyStr <- resp.body.asString
        suggestedRecipes <- ZIO.fromEither(decode[SuggestedRecipesResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(
           suggestedRecipes.recipes
             .find(_.id == recipe1Id)
             .is(_.some).available == recipe1Storage1Availability + recipe1Storage2Availability
         ) ?? "Recipe 1 exists and its 'available' fields are correct"
         && assertTrue(
           suggestedRecipes.recipes
             .find(_.id == recipe2Id)
             .is(_.some).available == recipe2Storage1Availability + recipe2Storage2Availability
         ) ?? "Recipe 2 exists and its 'available' fields are correct"
    },
    test("When querying with membered and owned storages, availabilities should be correct") {
      for
        creator <- registerUser
        memberedStorageId <- createStorage(creator)

        user <- registerUser
        _ <- ZIO.serviceWithZIO[StorageMembersRepo](_
          .addMemberToStorageById(memberedStorageId, user.userId)
        )

        storage1Id <- createStorage(user)
        storage2Id <- createStorage(user)

        minStorageIngredientsAmount = 4

        tmp <- ZIO.foreach(Seq(memberedStorageId, storage1Id, storage2Id))(storageId => for
          n <- Gen.int(3, 10).runHead.some
          ingredientIds <- createNIngredients(n)
          _ <- addIngredientsToStorage(storage1Id, ingredientIds)
        yield ingredientIds)
        Seq(memberedStorageIngredientIds, storage1IngredientIds, storage2IngredientIds) = tmp

        n <- Gen.int(2, 10).runHead.some
        otherIngredientIds <- createNIngredients(n)

        recipe1MemberedStorageAvailability = minStorageIngredientsAmount - 2
        recipe1Storage1Availability = minStorageIngredientsAmount
        recipe1Storage2Availability = minStorageIngredientsAmount - 1
        recipe1IngredientIds
          =  memberedStorageIngredientIds.takeRight(recipe1MemberedStorageAvailability)
          ++ storage1IngredientIds.take(recipe1Storage1Availability)
          ++ storage2IngredientIds.take(recipe1Storage2Availability)
          ++ otherIngredientIds
        recipe1Id <- createRecipe(user, recipe1IngredientIds)

        recipe2MemberedStorageAvailability = minStorageIngredientsAmount - 1
        recipe2Storage1Availability = minStorageIngredientsAmount - 2
        recipe2Storage2Availability = minStorageIngredientsAmount
        recipe2IngredientIds
          =  memberedStorageIngredientIds.takeRight(recipe2MemberedStorageAvailability)
          ++ storage1IngredientIds.takeRight(recipe2Storage1Availability)
          ++ storage2IngredientIds.takeRight(recipe2Storage2Availability)
          ++ otherIngredientIds
        recipe2Id <- createRecipe(user, recipe2IngredientIds)

        resp <- getSuggestedRecipes(user, Seq(storage1Id, storage2Id))

        bodyStr <- resp.body.asString
        suggestedRecipes <- ZIO.fromEither(decode[SuggestedRecipesResp](bodyStr))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(
           suggestedRecipes.recipes
             .find(_.id == recipe1Id)
             .is(_.some).available == recipe1MemberedStorageAvailability
                                    + recipe1Storage1Availability
                                    + recipe1Storage2Availability
         ) ?? "Recipe 1 exists and its 'available' fields are correct"
         && assertTrue(
           suggestedRecipes.recipes
             .find(_.id == recipe2Id)
             .is(_.some).available == recipe2MemberedStorageAvailability
                                    + recipe2Storage1Availability
                                    + recipe2Storage2Availability
         ) ?? "Recipe 2 exists and its 'available' fields are correct"
    },
  ).provideLayer(testLayer)

