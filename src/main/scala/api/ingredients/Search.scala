package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{InternalServerError, IngredientId, StorageId}

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO
import me.xdrop.fuzzywuzzy.FuzzySearch

private case class IngredientSearchResult(
                                         id: IngredientId,
                                         name: String,
                                         available: Boolean
                                         )

private val search: ZServerEndpoint[AppEnv, Any] =
  endpoint
    .get
    .in("ingredients-for-storage")
    .in(query[String]("query"))
    .in(query[StorageId]("storage"))
    .out(jsonBody[Vector[IngredientSearchResult]])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchHandler)

private def searchHandler(query: String, storageId: StorageId):
  ZIO[IngredientsRepo & StorageIngredientsRepo, InternalServerError, Vector[IngredientSearchResult]] =
  for
    allIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_.getAll).mapError(_ => InternalServerError())
    allIngredientsAvailability <- ZIO.foreach(allIngredients) {
      ingredient =>
        ZIO.serviceWithZIO[StorageIngredientsRepo](_.inStorage(storageId, ingredient.id)).debug
          .map(inStorage => IngredientSearchResult(ingredient.id, ingredient.name, inStorage)).debug
          .mapError(_ => InternalServerError())
    }.debug
    res = allIngredientsAvailability.sortBy(i => - FuzzySearch.tokenSetPartialRatio(query, i.name)) // negate the ratio to make order descending
  yield res