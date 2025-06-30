package api.recipes

import api.{
  AppEnv,
  handleFailedSqlQuery,
  toStorageNotFound
}
import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant
}
import db.DbError.{FailedDbQuery, DbNotRespondingError}
import db.repositories.RecipesDomainRepo
import domain.{InternalServerError, RecipeId, StorageId}
import domain.StorageError.NotFound

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

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
): ZIO[AppEnv, InternalServerError | NotFound, SuggestedRecipesResp] =
  {
    for
      suggestedTuples <- ZIO.serviceWithZIO[RecipesDomainRepo] {
        _.getSuggestedIngredients(size, offset, storageIds)
      }.catchSome {
        case e: FailedDbQuery => handleFailedSqlQuery(e).flatMap {
          toStorageNotFound(_, storageIds.head) // TODO: fix this костыль
            .flatMap(_ => ZIO.fail(InternalServerError()))
        }
      }
      suggested = suggestedTuples.map { (id, name, available, totalIngredients, _) =>
        SuggestedRecipeResp(id, name, available, totalIngredients)
      }
      recipesFound = suggestedTuples.collectFirst(_._5).getOrElse(0)
    yield SuggestedRecipesResp(recipesFound, suggested)
  }.mapError {
    case e: (DbNotRespondingError | InternalServerError) => InternalServerError()
    case e: NotFound => e
  }
