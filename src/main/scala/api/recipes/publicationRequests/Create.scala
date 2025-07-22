package api.recipes.publicationRequests

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

final case class CannotPublishRecipeWithPrivateIngredients(
  ingredients: Seq[IngredientId],
  message: String = "Cannot publish recipe with private ingredients",
)
object CannotPublishRecipeWithPrivateIngredients:
  val variant = BadRequest.variantJson[CannotPublishRecipeWithPrivateIngredients]

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

private type CreateEnv
  = RecipesRepo
  & RecipeIngredientsRepo
  & RecipePublicationRequestsRepo
  & DataSource

private val create: ZServerEndpoint[CreateEnv, Any] =
  recipesPublicationRequestsEndpoint
    .post
    .out(plainBody[PublicationRequestId] and statusCode(Created))
    .errorOut(oneOf(
      serverErrorVariant,
      recipeNotFoundVariant,
      CannotPublishRecipeWithPrivateIngredients.variant,
      RecipeAlreadyPending.variant,
      RecipeAlreadyPublished.variant,
    ))
    .zSecuredServerLogic(createHandler)

private def createHandler(recipeId: RecipeId):
  ZIO[
    AuthenticatedUser & CreateEnv,
    InternalServerError | RecipeAlreadyPublished | RecipeAlreadyPending
    | CannotPublishRecipeWithPrivateIngredients | RecipeNotFound,
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
    privateIngredientIdsInRecipe <- run(
      IngredientsQueries.privateIngredientsQ(lift(userId))
        .filter(i => liftQuery(recipe.ingredients).contains(i.id))
        .map(_.id)
    ).provideDS(using dataSource)
      .orElseFail(InternalServerError())

    _ <- ZIO.fail(CannotPublishRecipeWithPrivateIngredients(privateIngredientIdsInRecipe))
      .when(privateIngredientIdsInRecipe.nonEmpty)

    reqId <- ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_
      .createPublicationRequest(recipeId)
      .orElseFail(InternalServerError())
    )
  yield reqId
