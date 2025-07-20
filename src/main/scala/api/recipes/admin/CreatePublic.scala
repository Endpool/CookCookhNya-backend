package api.recipes.admin

import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import api.recipes.CreateRecipeReqBody
import api.{toIngredientNotFound, handleFailedSqlQuery}
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.QuillConfig.ctx.*
import db.QuillConfig.provideDS
import db.repositories.{RecipeIngredientsRepo, RecipesRepo, IngredientsQueries}
import domain.{IngredientNotFound, IngredientId, InternalServerError, RecipeId}

import io.circe.generic.auto.*
import io.getquill.*
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreatePublicEnv = RecipesRepo & RecipeIngredientsRepo & DataSource

private val createPublic: ZServerEndpoint[CreatePublicEnv, Any] =
  adminRecipesEndpoint
    .post
    .in(jsonBody[CreateRecipeReqBody])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zServerLogic(createPublicHandler)

private def createPublicHandler(recipeReq: CreateRecipeReqBody):
  ZIO[CreatePublicEnv, InternalServerError | IngredientNotFound, RecipeId] =
  val recipe = recipeReq.copy(ingredients=recipeReq.ingredients.distinct)
  for
    dataSource <- ZIO.service[DataSource]
    existingIngredientIds <- run(
      IngredientsQueries.publicIngredientsQ
        .map(_.id)
        .filter(id => liftQuery(recipe.ingredients).contains(id))
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())
    unknownIngredientIds = recipe.ingredients.diff(existingIngredientIds)
    _ <- ZIO.fail(IngredientNotFound(unknownIngredientIds.head.toString))
      .when(unknownIngredientIds.nonEmpty)

    recipeId <- ZIO.serviceWithZIO[RecipesRepo](_
      .createPublic(recipe.name, recipe.sourceLink, recipe.ingredients)
      .mapError {
        case _: DbNotRespondingError => InternalServerError()
        case e: FailedDbQuery => handleFailedSqlQuery(e)
          .flatMap(toIngredientNotFound)
          .getOrElse(InternalServerError())
      }
    )
  yield recipeId
