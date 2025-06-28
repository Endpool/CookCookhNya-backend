package api.recipes

import api.AppEnv
import api.EndpointErrorVariants.{serverErrorVariant, storageNotFoundVariant}
import db.repositories.RecipesDomainRepo
import domain.{DbError, RecipeId, StorageError, StorageId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

private case class SuggestedRecipeResp(id: RecipeId, name: String, available: Int, total: Int)
private case class SuggestedRecipesResp(recipesFound: Int, recipes: Vector[SuggestedRecipeResp])

val getSuggested =
  recipesEndpoint
    .get
    .in(query[Int]("size").default(2))
    .in(query[Int]("offset").default(0))
    .in(query[Vector[StorageId]]("storageId"))
    .out(jsonBody[SuggestedRecipesResp])
    .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
    .zServerLogic(getSuggestedHandler)

private def getSuggestedHandler(
  size: Int,
  offset: Int,
  storageIds: Vector[StorageId]
): ZIO[AppEnv, DbError.UnexpectedDbError | StorageError.NotFound, SuggestedRecipesResp] =
  for
    suggestedTuples <- ZIO.serviceWithZIO[RecipesDomainRepo] {
      _.getSuggestedIngredients(size, offset, storageIds)
    }
    suggested = suggestedTuples.map{ (id, name, available, totalIngredients, _) =>
      SuggestedRecipeResp(id, name, available, totalIngredients)
    }
    recipesFound = suggestedTuples.collectFirst(_._5).getOrElse(0)
  yield SuggestedRecipesResp(recipesFound, suggested)
