package api.recipes.publicationRequests

import sttp.tapir.ztapir.*

import api.recipes.{recipesEndpoint}
import api.TapirExtensions.subTag
import domain.RecipeId

val recipesPublicationRequestsEndpoint =
  recipesEndpoint
    .subTag("Publication Requests")
    .in(path[RecipeId]("recipeId") / "publication-requests")

val recipesPublicationRequestsEndpoints = List(
  create.widen,
  getAll.widen,
)
