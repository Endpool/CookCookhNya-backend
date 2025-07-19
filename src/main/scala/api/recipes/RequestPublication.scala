package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant}
import api.variantJson
import db.QuillConfig.ctx.*
import db.QuillConfig.provideDS
import db.repositories.{RecipePublicationRequestsQueries, IngredientsQueries, RecipeIngredientsRepo, RecipePublicationRequestsRepo, RecipesRepo}
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound, PublicationRequestId}

import io.circe.generic.auto.*
import io.getquill.*
import javax.sql.DataSource
import sttp.model.StatusCode.{BadRequest, Created}
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

final case class RecipeAlreadyPending(
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
    .out(plainBody[PublicationRequestId] and statusCode(Created))
    .errorOut(oneOf(
      serverErrorVariant,
      recipeNotFoundVariant,
      CannotPublishRecipeWithCustomIngredients.variant,
      RecipeAlreadyPending.variant,
      RecipeAlreadyPublished.variant,
    ))
    .zSecuredServerLogic(requestPublicationHandler)

private def requestPublicationHandler(recipeId: RecipeId):
  ZIO[
    AuthenticatedUser & PublishEnv,
    InternalServerError | RecipeAlreadyPublished | RecipeAlreadyPending
    | CannotPublishRecipeWithCustomIngredients | RecipeNotFound,
    PublicationRequestId
  ] =
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
        .pendingRequestsByRecipeIdQ(lift(recipeId)).nonEmpty
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())
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

    reqId <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .createPublicationRequest(recipeId)
      .orElseFail(InternalServerError())
    )
  yield reqId
