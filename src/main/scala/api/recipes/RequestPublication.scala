package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant}
import api.variantJson
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound}
import db.repositories.{RecipePublicationRequestsQueries, IngredientsQueries, RecipeIngredientsRepo, RecipePublicationRequestsRepo, RecipesRepo}
import db.QuillConfig.provideDS
import db.QuillConfig.ctx.*
import io.circe.generic.auto.*
import io.getquill.*

import javax.sql.DataSource
import sttp.model.StatusCode.{BadRequest, NoContent}
import sttp.tapir.generic.auto.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class CannotPublishRecipeWithCustomIngredients(
  ingredients: Seq[IngredientId],
  message: String = "Cannot publish recipe with custom ingredients",
)
object CannotPublishRecipeWithCustomIngredients:
  val variant = BadRequest.variantJson[CannotPublishRecipeWithCustomIngredients]

final case class RecipeAlreadyPublished(
  recipeId: RecipeId,
  message: String = "Recipe already published",
)
object RecipeAlreadyPublished:
  val variant = BadRequest.variantJson[RecipeAlreadyPublished]


private final case class RecipeAlreadyPending(
  recipeId: RecipeId,
  message: String = "Recipe already pending"
)

object RecipeAlreadyPending:
  val variant = BadRequest.variantJson[RecipeAlreadyPending]

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
      CannotPublishRecipeWithCustomIngredients.variant,
      RecipeAlreadyPending.variant,
      RecipeAlreadyPublished.variant,
    ))
    .out(statusCode(NoContent))
    .zSecuredServerLogic(requestPublicationHandler)

private def requestPublicationHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & PublishEnv,
      InternalServerError | RecipeAlreadyPublished | RecipeAlreadyPending |
      CannotPublishRecipeWithCustomIngredients | RecipeNotFound,
      Unit] =
  for
    recipe <- ZIO.serviceWithZIO[RecipesRepo](_
      .getRecipe(recipeId)
      .some.orElseFail(RecipeNotFound(recipeId))
    )
    _ <- ZIO.fail(RecipeAlreadyPublished(recipeId))
      .when(recipe.isPublished)

    dataSource <- ZIO.service[DataSource]
    alreadyPending <- run(
      RecipePublicationRequestsQueries
        .pendningRequestsByIdQ(lift(recipeId))
    )
      .provideDS(using dataSource)
      .orElseFail(InternalServerError())
      .map(_.nonEmpty)
    _ <- ZIO.fail(RecipeAlreadyPending(recipeId))
      .when(alreadyPending)

    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    customIngredientIdsInRecipe <- run(
      IngredientsQueries.customIngredientsQ(lift(userId))
        .filter(i => liftQuery(recipe.ingredients).contains(i.id))
        .map(_.id)
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())

    _ <- ZIO.fail(CannotPublishRecipeWithCustomIngredients(customIngredientIdsInRecipe))
      .when(customIngredientIdsInRecipe.nonEmpty)

    _ <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .requestPublication(recipeId)
      .orElseFail(InternalServerError())
    )
  yield ()
