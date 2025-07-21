package api.recipes

import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*
import api.recipes.ingredients.recipesIngredientsEndpoints
import api.recipes.publicationRequests.recipesPublicationRequestsEndpoints
import api.recipes.admin.adminRecipesEndpoints
import api.recipes.public.publicRecipesEndpoints

val recipesEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] =
  recipesEndpoint()

def recipesEndpoint(path: String = "recipes"): Endpoint[Unit, Unit, Unit, Unit, Any] =
  endpoint
    .tag("Recipes")
    .in(path)

val recipeEndpoints = List(
  create.widen,
  getSuggested.widen,
  get.widen,
  searchAll.widen,
  delete.widen,
) ++ recipesIngredientsEndpoints.map(_.widen)
  ++ recipesPublicationRequestsEndpoints.map(_.widen)
  ++ adminRecipesEndpoints.map(_.widen)
  ++ publicRecipesEndpoints.map(_.widen)
