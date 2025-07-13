package api.recipes

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant}
import api.variantJson
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound}
import db.repositories.{IngredientsQueries, RecipeIngredientsRepo, RecipePublicationRequestsRepo, RecipesRepo}
import db.QuillConfig.provideDS
import db.QuillConfig.ctx.*

import io.circe.generic.auto.*
import io.getquill.*
import javax.sql.DataSource
import sttp.model.StatusCode.{NoContent, BadRequest}
import sttp.tapir.generic.auto.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class CannotPublishRecipeWithPersonalIngredients(ingredients: Seq[IngredientId])
object CannotPublishRecipeWithPersonalIngredients:
  val variant = BadRequest.variantJson[CannotPublishRecipeWithPersonalIngredients]

final case class CannotPublishPublishedRecipe(recipeId: RecipeId)
object CannotPublishPublishedRecipe:
  val variant = BadRequest.variantJson[CannotPublishPublishedRecipe]

private type PublishEnv
  = RecipesRepo
  & RecipeIngredientsRepo
  & RecipePublicationRequestsRepo
  & DataSource

private val publish: ZServerEndpoint[PublishEnv, Any] =
  recipesEndpoint
    .post
    .in(path[RecipeId]("recipeId") / "publish")
    .errorOut(oneOf(
      serverErrorVariant,
      recipeNotFoundVariant,
      CannotPublishRecipeWithPersonalIngredients.variant,
      CannotPublishPublishedRecipe.variant,
    ))
    .out(statusCode(NoContent))
    .zSecuredServerLogic(publishHandler)

private def publishHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & PublishEnv,
      InternalServerError | CannotPublishPublishedRecipe |
      CannotPublishRecipeWithPersonalIngredients | RecipeNotFound,
      Unit] =
  for
    recipe <- ZIO.serviceWithZIO[RecipesRepo](_
      .getRecipe(recipeId)
      .some.orElseFail(RecipeNotFound(recipeId.toString))
    )
    _ <- ZIO.fail(CannotPublishPublishedRecipe(recipeId))
      .when(recipe.isPublished)

    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    dataSource <- ZIO.service[DataSource]
    personalIngredientIdsInRecipe <- run(
      IngredientsQueries.getAllPersonalQ(lift(userId))
        .filter(i => liftQuery(recipe.ingredients).contains(i.id))
        .map(_.id)
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())

    _ <- ZIO.fail(CannotPublishRecipeWithPersonalIngredients(personalIngredientIdsInRecipe))
      .when(personalIngredientIdsInRecipe.nonEmpty)

    _ <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .publish(recipeId)
      .orElseFail(InternalServerError())
    )
  yield ()
