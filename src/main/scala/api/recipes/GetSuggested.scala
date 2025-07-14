package api.recipes

import api.EndpointErrorVariants.{
  serverErrorVariant,
  storageNotFoundVariant
}
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.PaginationParams
import db.repositories.{RecipesDomainRepo, StorageIngredientsRepo, StoragesRepo}
import domain.{InternalServerError, RecipeId, StorageId, StorageNotFound}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class SuggestedRecipeResp(id: RecipeId, name: String, available: Int, total: Int)
final case class SuggestedRecipesResp(found: Int, recipes: Vector[SuggestedRecipeResp])

private type GetSuggestedEnv = RecipesDomainRepo & StorageIngredientsRepo & StoragesRepo

private val getSuggested: ZServerEndpoint[GetSuggestedEnv, Any] =
  recipesEndpoint(
    path = "suggested-recipes"
  ).get
    .in(PaginationParams.query)
    .in(query[Vector[StorageId]]("storage-id"))
    .out(jsonBody[SuggestedRecipesResp])
    .errorOut(oneOf(serverErrorVariant, storageNotFoundVariant))
    .zSecuredServerLogic(getSuggestedHandler)

private def getSuggestedHandler(
  paginationParams: PaginationParams,
  storageIds: Vector[StorageId]
): ZIO[AuthenticatedUser & GetSuggestedEnv,
       InternalServerError | StorageNotFound,
       SuggestedRecipesResp] = for
  _ <- ZIO.serviceWithZIO[StoragesRepo](repo =>
    ZIO.foreach(storageIds)(storageId => repo
      .getById(storageId)
      .some.orElseFail(StorageNotFound(storageId.toString))
    )
  )
  suggestedTuples <- ZIO.serviceWithZIO[RecipesDomainRepo](_
    .getSuggestedIngredients(paginationParams, storageIds)
    .orElseFail(InternalServerError())
  )
  suggested = suggestedTuples.map { (id, name, available, totalIngredients, _) =>
    SuggestedRecipeResp(id, name, available, totalIngredients)
  }
  recipesFound = suggestedTuples.collectFirst(_._5).getOrElse(0)
yield SuggestedRecipesResp(recipesFound, suggested)
