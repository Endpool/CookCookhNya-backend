package api.recipes.ingredients

import api.EndpointErrorVariants.{
  ingredientNotFoundVariant,
  serverErrorVariant,
  storageNotFoundVariant
}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import common.OptionExtensions.<|>
import db.DbError.{DbNotRespondingError, FailedDbQuery}
import db.repositories.StorageIngredientsRepo
import domain.{IngredientNotFound, IngredientId, InternalServerError, StorageNotFound, StorageId}

import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.ZIO

private type AddEnv = StorageIngredientsRepo

private val add: ZServerEndpoint[AddEnv, Any] =
  recipesIngredientsEndpoint
    .put
    .in(path[IngredientId]("ingredientId"))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(oneOf(
      serverErrorVariant,
      ingredientNotFoundVariant,
      storageNotFoundVariant,
    ))
    .zSecuredServerLogic(addHandler)

private def addHandler(storageId : StorageId, ingredientId: IngredientId):
  ZIO[AuthenticatedUser & AddEnv,
      InternalServerError | IngredientNotFound | StorageNotFound,
      Unit] = ZIO.unit

