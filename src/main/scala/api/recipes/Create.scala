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

final case class CreateRecipeReqBody(
  name: String,
  sourceLink: Option[String],
  ingredients: List[IngredientId]
)

private type CreateEnv = RecipesRepo & RecipeIngredientsRepo & DataSource

private val create: ZServerEndpoint[CreateEnv, Any] =
  recipesEndpoint
    .post
    .in(jsonBody[CreateRecipeReqBody])
    .out(plainBody[RecipeId])
    .errorOut(oneOf(serverErrorVariant, ingredientNotFoundVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(recipe: CreateRecipeReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError | IngredientNotFound, RecipeId] = for
  userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
  dataSource <- ZIO.service[DataSource]
  existingIngredientIds <- run(
    IngredientsQueries.getAllVisibleQ(lift(userId))
      .map(_.id)
      .filter(id => liftQuery(recipe.ingredients).contains(id))
  ).provideDS(using dataSource).orElseFail(InternalServerError())
  uknownIngredientIds = recipe.ingredients.diff(existingIngredientIds)
  _ <- ZIO.fail(IngredientNotFound(uknownIngredientIds.head.toString))
    .when(uknownIngredientIds.nonEmpty)

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
