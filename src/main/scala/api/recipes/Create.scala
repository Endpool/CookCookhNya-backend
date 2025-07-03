package api.recipes

import api.{AppEnv, failIfIngredientNotFound, handleFailedSqlQuery}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.{RecipeIngredientsRepo, RecipesRepo}
import domain.{IngredientId, InternalServerError, RecipeId}
import domain.IngredientError.NotFound
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private final case class RecipeCreationEntity(name: String, sourceLink: String, ingredients: Vector[IngredientId])

val create: ZServerEndpoint[AppEnv, Any] =
  recipesEndpoint
    .post
    .in(jsonBody[RecipeCreationEntity])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zServerLogic(createHandler)

def createHandler(recipe: RecipeCreationEntity):
ZIO[RecipesRepo & RecipeIngredientsRepo, InternalServerError | NotFound, RecipeId] =
  ZIO.serviceWithZIO[RecipesRepo] {
    _.addRecipe(recipe.name, recipe.sourceLink, recipe.ingredients)
  }.catchAll {
    case DbNotRespondingError(_) => ZIO.fail(InternalServerError())
    case e: FailedDbQuery => for {
      missingEntry <- handleFailedSqlQuery(e)
      (keyName, keyValue, _) = missingEntry
      _ <- failIfIngredientNotFound(keyName, keyValue)
      res <- ZIO.fail(InternalServerError())
    } yield res 
  }
