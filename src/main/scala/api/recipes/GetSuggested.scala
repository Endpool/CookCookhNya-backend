package api.recipes

import api.{
  handleFailedSqlQuery,
  toStorageNotFound,
  ForeignKeyViolation,
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant
}
import db.DbError.{FailedDbQuery, DbNotRespondingError}
import db.repositories.RecipesDomainRepo
import domain.{InternalServerError, RecipeId, StorageId, StorageNotFound}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import db.repositories.StorageIngredientsRepo
import zio.RIO
import sttp.tapir.server.ServerEndpoint

case class SuggestedRecipeResp(id: RecipeId, name: String, available: Int, total: Int)
case class SuggestedRecipesResp(recipesFound: Int, recipes: Vector[SuggestedRecipeResp])

private type GetSuggestedEnv = RecipesDomainRepo & StorageIngredientsRepo

private val getSuggested: ZServerEndpoint[GetSuggestedEnv, Any] =
  recipesEndpoint
    .in("suggested")
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
): ZIO[GetSuggestedEnv, InternalServerError | StorageNotFound, SuggestedRecipesResp] = {
  for
    suggestedTuples <- ZIO.serviceWithZIO[RecipesDomainRepo] {
      _.getSuggestedIngredients(size, offset, storageIds)
    }
    suggested = suggestedTuples.map { (id, name, available, totalIngredients, _) =>
      SuggestedRecipeResp(id, name, available, totalIngredients)
    }
    recipesFound = suggestedTuples.collectFirst(_._5).getOrElse(0)
  yield SuggestedRecipesResp(recipesFound, suggested)
}.mapError {
  case e: FailedDbQuery => handleFailedSqlQuery(e)
    .flatMap(toStorageNotFound)
    .getOrElse(InternalServerError())
  case _: DbNotRespondingError => InternalServerError()
}
