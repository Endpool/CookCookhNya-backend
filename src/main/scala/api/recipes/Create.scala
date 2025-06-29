package api.recipes

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{RecipeIngredientsRepo, RecipesRepo}
import domain.{InternalServerError, IngredientId}
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
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(createHandler)

def createHandler(recipe: RecipeCreationEntity): ZIO[RecipesRepo & RecipeIngredientsRepo, InternalServerError, Unit] =
  ZIO.serviceWithZIO[RecipesRepo] {
    _.addRecipe(recipe.name, recipe.sourceLink, recipe.ingredients)
  }.mapError(_ => InternalServerError())
