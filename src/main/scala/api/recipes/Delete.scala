package api.recipes

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeAccessForbiddenVariant, serverErrorVariant}
import db.repositories.RecipesRepo
import domain.{InternalServerError, RecipeId}
import domain.RecipeAccessForbidden

import sttp.tapir.ztapir.*
import sttp.model.StatusCode.NoContent
import zio.ZIO

private type DeleteEnv = RecipesRepo

private val delete: ZServerEndpoint[DeleteEnv, Any] =
  recipesEndpoint
    .delete
    .in(path[RecipeId]("recipeId"))
    .out(statusCode(NoContent))
    .errorOut(oneOf(serverErrorVariant, recipeAccessForbiddenVariant))
    .zSecuredServerLogic(deleteHandler)

private def deleteHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & DeleteEnv, InternalServerError | RecipeAccessForbidden, Unit] = for
  userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
  mRecipe <- ZIO.serviceWithZIO[RecipesRepo](_
    .getRecipe(recipeId)
    .orElseFail(InternalServerError())
  )
  _ <- mRecipe match
    case None => ZIO.unit
    case Some(recipe) =>
      if recipe.creatorId == userId then
        ZIO.serviceWithZIO[RecipesRepo](_
          .deleteRecipe(recipeId)
          .orElseFail(InternalServerError())
        )
      else
        ZIO.fail(RecipeAccessForbidden(recipeId))
yield ()
