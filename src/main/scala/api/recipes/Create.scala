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

final case class CreateRecipeReqBody(
  name: String,
  sourceLink: String,
  ingredients: Vector[IngredientId]
)

val create: ZServerEndpoint[AppEnv, Any] =
  recipesEndpoint
    .post
    .in(jsonBody[RecipeCreationEntity])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zServerLogic(createHandler)

private def createHandler(recipe: CreateRecipeReqBody):
  ZIO[RecipesRepo & RecipeIngredientsRepo, InternalServerError | NotFound, Unit] =
  ZIO.serviceWithZIO[RecipesRepo] {
    _.addRecipe(recipe.name, recipe.sourceLink, recipe.ingredients)
  }.mapError {
    case DbNotRespondingError(_) => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toIngredientNotFound)
      .getOrElse(InternalServerError())
  }
