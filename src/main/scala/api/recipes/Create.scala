package api.recipes

import api.{toIngredientNotFound, handleFailedSqlQuery}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.{RecipeIngredientsRepo, RecipesRepo}
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId}

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

private type CreateEnv = RecipesRepo & RecipeIngredientsRepo

private val create: ZServerEndpoint[CreateEnv, Any] =
  recipesEndpoint
    .post
    .in(jsonBody[CreateRecipeReqBody])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zServerLogic(createHandler)

private def createHandler(recipe: CreateRecipeReqBody):
  ZIO[CreateEnv, InternalServerError | IngredientNotFound, RecipeId] =
  ZIO.serviceWithZIO[RecipesRepo] {
    _.addRecipe(recipe.name, recipe.sourceLink, recipe.ingredients)
  }.mapError {
    case DbNotRespondingError(_) => InternalServerError()
    case e: FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toIngredientNotFound)
      .getOrElse(InternalServerError())
  }
