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

final case class CannotPublishRecipeWithPersonalIngredients(
  ingredients: Seq[IngredientId],
  message: String = "Cannot publish recipe with personal ingredients",
)
object CannotPublishRecipeWithPersonalIngredients:
  val variant = BadRequest.variantJson[CannotPublishRecipeWithPersonalIngredients]

final case class CannotPublishPublishedRecipe(
  recipeId: RecipeId,
  message: String = "Cannot publish recipe that is published",
)
object CannotPublishPublishedRecipe:
  val variant = BadRequest.variantJson[CannotPublishPublishedRecipe]

private type PublishEnv
  = RecipesRepo
  & RecipeIngredientsRepo
  & RecipePublicationRequestsRepo
  & DataSource

private val requestPublication: ZServerEndpoint[PublishEnv, Any] =
  recipesEndpoint
    .post
    .in(path[RecipeId]("recipeId") / "request-publication")
    .errorOut(oneOf(
      serverErrorVariant,
      recipeNotFoundVariant,
      CannotPublishRecipeWithPersonalIngredients.variant,
      CannotPublishPublishedRecipe.variant,
    ))
    .out(statusCode(NoContent))
    .zSecuredServerLogic(requestPublicationHandler)

private def requestPublicationHandler(recipeId: RecipeId):
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
      IngredientsQueries.getAllCustomQ(lift(userId))
        .filter(i => liftQuery(recipe.ingredients).contains(i.id))
        .map(_.id)
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())

    _ <- ZIO.fail(CannotPublishRecipeWithPersonalIngredients(personalIngredientIdsInRecipe))
      .when(personalIngredientIdsInRecipe.nonEmpty)

    _ <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .requestPublication(recipeId)
      .orElseFail(InternalServerError())
    )
  yield ()
