package integration.api.recipes

import api.Authentication.AuthenticatedUser
import api.recipes.CreateRecipeReqBody
import db.repositories.{IngredientsRepo, RecipesRepo}
import db.tables.recipesTable
import domain.{IngredientId, IngredientNotFound}
import integration.common.Utils.*
import integration.common.ZIOIntegrationTestSpec

import com.augustnagro.magnum.magzio.{Transactor, sql}
import io.circe.parser.*
import io.circe.generic.auto.*
import zio.http.{Client, Path, Status, URL}
import zio.{Scope, ZIO, RIO}
import zio.http.Response
import zio.test.*
import zio.Cause

object CreateRecipeTests extends ZIOIntegrationTestSpec:
  private val endpointPath = URL(Path.root / "recipes")

  private def createRecipe(user: AuthenticatedUser, reqBody: CreateRecipeReqBody):
    RIO[Client, Response] =
    Client.batched(
      post(endpointPath)
        .withJsonBody(reqBody)
        .addAuthorization(user)
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("Create recipe tests")(
    test("When unauthorized should get 401") {
      Client.batched(
        post(endpointPath)
          .withJsonBody(CreateRecipeReqBody("recipe", Some("sourceLink"), List.empty))
      ).map(resp => assertTrue(resp.status == Status.Unauthorized))
    },
    test("When authorized should get 200") {
      for
        user <- registerUser
        resp <- createRecipe(user, CreateRecipeReqBody("recipe", Some("sourceLink"), List.empty))
      yield assertTrue(resp.status == Status.Ok)
    },
    test("When create valid recipe with public ingredients, recipe should be added to db") {
      for
        recipeName <- randomString
        recipeSourceLink <- randomString.map(Some(_))
        ingredientIds <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
          Gen.alphaNumericString.runCollectN(10).flatMap(ZIO.foreach(_)(repo.addPublic))
        ).map(_.map(_.id))
          .map(Vector.from)

        user <- registerUser

        resp <- createRecipe(user, CreateRecipeReqBody(recipeName, recipeSourceLink, ingredientIds.toList))
        bodyStr <- resp.body.asString
        recipeId <- ZIO.fromOption(bodyStr.toUUID)
          .orElse(failed(Cause.fail(s"Could not parse response recipeId $bodyStr")))
        recipe <- ZIO.serviceWithZIO[RecipesRepo](_.getRecipe(recipeId).provideUser(user))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(recipe.is(_.some).name == recipeName)
         && assertTrue(recipe.is(_.some).sourceLink == recipeSourceLink)
         && assertTrue(recipe.get.ingredients hasSameElementsAs ingredientIds)
    },
    test("When create valid recipe with public and custom ingredients, recipe should be added to db") {
      for
        user <- registerUser
        recipeName <- randomString
        recipeSourceLink <- randomString.map(Some(_))
        ingredientIds <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
          for
            publicIngredientIds   <- Gen.alphaNumericString.runCollectN(10)
              .flatMap(ZIO.foreach(_)(repo.addPublic))
              .map(_.map(_.id))
              .map(Vector.from)
            customIngredientIds <- Gen.alphaNumericString.runCollectN(10)
              .flatMap(ZIO.foreach(_)(repo.addCustom)).provideUser(user)
              .map(_.map(_.id))
              .map(Vector.from)
          yield publicIngredientIds ++ customIngredientIds
        )

        resp <- createRecipe(user, CreateRecipeReqBody(recipeName, recipeSourceLink, ingredientIds.toList))
        bodyStr <- resp.body.asString
        recipeId <- ZIO.fromOption(bodyStr.toUUID)
          .orElse(failed(Cause.fail(s"Could not parse response recipeId $bodyStr")))
        recipe <- ZIO.serviceWithZIO[RecipesRepo](_.getRecipe(recipeId).provideUser(user))
      yield assertTrue(resp.status == Status.Ok)
         && assertTrue(recipe.is(_.some).name == recipeName)
         && assertTrue(recipe.is(_.some).sourceLink == recipeSourceLink)
         && assertTrue(recipe.get.ingredients hasSameElementsAs ingredientIds)
    },
    test("When create recipe with non-existant ingredients, should get 404 ingredient not found and recipe should NOT be added to db") {
      for
        recipeName <- randomString
        recipeSourceLink <- randomString.map(Some(_))
        ingredientIds <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
          for
            publicIngredientIds <- Gen.alphaNumericString.runCollectN(10)
              .flatMap(ZIO.foreach(_)(repo.addPublic))
              .map(_.map(_.id))
              .map(Vector.from)
            nonExistantIngredientIds <- Gen.uuid.runCollectN(10)
          yield publicIngredientIds ++ nonExistantIngredientIds
        )

        user <- registerUser

        resp <- createRecipe(user, CreateRecipeReqBody(recipeName, recipeSourceLink, ingredientIds.toList))
        bodyStr <- resp.body.asString
        ingredientNotFound = decode[IngredientNotFound](bodyStr)
        recipeDoesNotExist <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${recipesTable.name} FROM $recipesTable
            WHERE ${recipesTable.name} = $recipeName
          """.query[String].run().isEmpty
        ))
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(ingredientNotFound.isRight)
         && assertTrue(recipeDoesNotExist)
    },
    test("When create recipe with other user's custom ingredients, should get 404 ingredient not found and recipe should NOT be added to db") {
      for
        otherUser <- registerUser
        recipeName <- randomString
        recipeSourceLink <- randomString.map(Some(_))
        ingredientIds <- ZIO.serviceWithZIO[IngredientsRepo](repo =>
          for
            publicIngredientIds   <- Gen.alphaNumericString.runCollectN(10)
              .flatMap(ZIO.foreach(_)(repo.addPublic))
              .map(_.map(_.id))
              .map(Vector.from)
            customIngredientIds <- Gen.alphaNumericString.runCollectN(10)
              .flatMap(ZIO.foreach(_)(repo.addCustom)).provideUser(otherUser)
              .map(_.map(_.id))
              .map(Vector.from)
          yield publicIngredientIds ++ customIngredientIds
        )

        user <- registerUser

        resp <- createRecipe(user, CreateRecipeReqBody(recipeName, recipeSourceLink, ingredientIds.toList))
        bodyStr <- resp.body.asString
        ingredientNotFound = decode[IngredientNotFound](bodyStr)
        recipeDoesNotExist <- ZIO.serviceWithZIO[Transactor](_.transact(
          sql"""
            SELECT ${recipesTable.name} FROM $recipesTable
            WHERE ${recipesTable.name} = $recipeName
          """.query[String].run().isEmpty
        ))
      yield assertTrue(resp.status == Status.NotFound)
         && assertTrue(ingredientNotFound.isRight)
         && assertTrue(recipeDoesNotExist)
    },
  ).provideLayer(testLayer)

