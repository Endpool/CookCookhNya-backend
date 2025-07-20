package api.recipes.public

import api.ingredients.IngredientResp
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant, userNotFoundVariant}
import db.QuillConfig.ctx.*
import db.QuillConfig.provideDS
import db.repositories.{IngredientsQueries, RecipeIngredientsQueries, RecipesQueries}
import db.tables.{DbIngredient, DbRecipe, DbUser}
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound, UserId}

import io.circe.generic.auto.*
import io.getquill.{query => quillQuery, *}
import java.sql.SQLException
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class RecipeCreatorResp(
  id: UserId,
  fullName: String
)

final case class RecipeResp(
  ingredients: Seq[IngredientResp],
  name: String,
  sourceLink: Option[String],
  creator: Option[RecipeCreatorResp],
)

private type GetPublicEnv = DataSource

private val getPublic: ZServerEndpoint[GetPublicEnv, Any] =
  publicRecipesEndpoint
    .get
    .in(path[RecipeId]("recipeId"))
    .errorOut(oneOf(serverErrorVariant, recipeNotFoundVariant, userNotFoundVariant))
    .out(jsonBody[RecipeResp])
    .zServerLogic(getPublicHandler)

private def getPublicHandler(recipeId: RecipeId):
  ZIO[GetPublicEnv, InternalServerError | RecipeNotFound, RecipeResp] = for
    dataSource <- ZIO.service[DataSource]
    recipe <- getRecipe(recipeId).provideDS(using dataSource)
      .orElseFail(InternalServerError())
      .someOrFail(RecipeNotFound(recipeId))
  yield recipe

def getRecipe(recipeId: RecipeId): ZIO[DataSource, Throwable, Option[RecipeResp]] = transaction {
  for
    mRecipe <- run(
      RecipesQueries.getPublicRecipeQ(lift(recipeId))
        .leftJoin(quillQuery[DbUser])
        .on(_.creatorId contains _.id)
        .value
    )
    mRecipeWithIngredients <- ZIO.foreach(mRecipe) { (recipe, mCreator) =>
      val DbRecipe(id, name, creatorId, isPublished, sourceLink) = recipe
      val creatorResp = mCreator.map(creator => RecipeCreatorResp(creator.id, creator.fullName))
      run(
        RecipeIngredientsQueries.getAllIngredientsQ(lift(recipeId))
          .join(IngredientsQueries.publicIngredientsQ)
          .on(_ == _.id)
          .map(_._2)
      ).map(_.map{case DbIngredient(id, _, name, _) => IngredientResp(id, name)})
        .map(RecipeResp(_, name, sourceLink, creatorResp))
    }
  yield mRecipeWithIngredients
}
