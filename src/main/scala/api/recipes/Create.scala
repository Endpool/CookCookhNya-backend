package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import api.{toIngredientNotFound, handleFailedSqlQuery}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.QuillConfig.ctx.*
import db.QuillConfig.provideDS
import db.repositories.{RecipeIngredientsRepo, RecipesRepo}
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId}

import io.getquill.*
import io.circe.generic.auto.*
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import db.repositories.IngredientsQueries

private type CreateEnv = RecipesRepo & RecipeIngredientsRepo & DataSource

private val create: ZServerEndpoint[CreateEnv, Any] =
  recipesEndpoint
    .post
    .in(jsonBody[CreateRecipeReqBody])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(recipeReq: CreateRecipeReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError | IngredientNotFound, RecipeId] =
  val recipe = recipeReq.copy(ingredients=recipeReq.ingredients.distinct)
  for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    dataSource <- ZIO.service[DataSource]
    existingIngredientIds <- run(
      IngredientsQueries.visibleIngredientsQ(lift(userId))
        .map(_.id)
        .filter(id => liftQuery(recipe.ingredients).contains(id))
    ).provideDS(using dataSource).orElseFail(InternalServerError())
    unknownIngredientIds = recipe.ingredients.diff(existingIngredientIds)
    _ <- ZIO.fail(IngredientNotFound(unknownIngredientIds.head.toString))
      .when(unknownIngredientIds.nonEmpty)

    recipeId <- ZIO.serviceWithZIO[RecipesRepo](_
      .addRecipe(recipe.name, recipe.sourceLink, recipe.ingredients)
      .mapError {
        case DbNotRespondingError(_) => InternalServerError()
        case e: FailedDbQuery => handleFailedSqlQuery(e)
          .flatMap(toIngredientNotFound)
          .getOrElse(InternalServerError())
      }
    )
  yield recipeId
