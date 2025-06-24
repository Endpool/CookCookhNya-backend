package api.recipes

import api.{AppEnv, EndpointErrorVariants}
import db.repositories.{RecipeIngredientsRepo, RecipesRepo, StorageIngredientsRepo}
import domain.{DbError, IngredientId, RecipeId, StorageError, StorageId}
import db.tables.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

private case class RecipeSummary(id: RecipeId, name: String, available: Int, total: Int)
private case class EndpointOutput(recipesFound: Int, recipes: Vector[RecipeSummary])

val getSuggested =
  endpoint
    .get
    .in("recipes")
    .in(query[Int]("size").default(2))
    .in(query[Int]("offset").default(0))
    .in(query[Vector[StorageId]]("storageId"))
    .out(jsonBody[EndpointOutput])
    .errorOut(oneOf(EndpointErrorVariants.serverErrorVariant, EndpointErrorVariants.storageNotFoundVariant))
    .zServerLogic(getSuggestedHandler)

private def getSuggestedHandler(
                                 size: Int,
                                 offset: Int,
                                 storageIds: Vector[StorageId]
                               ): ZIO[AppEnv, DbError.UnexpectedDbError | StorageError.NotFound, EndpointOutput] =
  for
    suggestedTuples <- ZIO.serviceWithZIO[RecipeIngredientsRepo](
      _.getSuggestedIngredients(size, offset, storageIds)
    )
    suggested = suggestedTuples.map(
      (id, name, available, total) => RecipeSummary(id, name, available, total)
    )
    res = EndpointOutput(suggested.length, suggested)
  yield res
