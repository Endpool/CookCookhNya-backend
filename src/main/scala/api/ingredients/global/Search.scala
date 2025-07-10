package api.ingredients.global

import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.{IngredientResp, SearchAllResultsResp}
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.{query, *}
import zio.ZIO

private type SearchAllEnv = IngredientsRepo & StorageIngredientsRepo

private val searchGlobal: ZServerEndpoint[SearchAllEnv, Any] =
  globalIngredientsEndpoint
  .get
  .in(query[String]("query"))
  .in(query[Int]("size").default(2))
  .in(query[Int]("offset").default(0))
  .in(query[Int]("threshold").default(50))
  .out(jsonBody[SearchAllResultsResp])
  .errorOut(oneOf(serverErrorVariant))
  .zServerLogic(searchGlobalHandler)

private def searchGlobalHandler(
  query: String,
  size: Int,
  offset: Int,
  threshold: Int
): ZIO[SearchAllEnv, InternalServerError, SearchAllResultsResp] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo] (_.getAllGlobal.orElseFail(InternalServerError()))
    allIngredients = allDbIngredients.map(dbIngredient => IngredientResp(dbIngredient.id, dbIngredient.name))
    res = allIngredients
      .map(i => (i, FuzzySearch.tokenSetPartialRatio(query, i.name)))
      .filter((_, ratio) => ratio >= threshold)
      .sortBy(
        (i, ratio) => (
          -ratio, // negate the ratio to make order descending
          (i.name.length - query.length).abs // secondary sort by length difference
        )
      )
      .map(_._1)
  yield SearchAllResultsResp(res.slice(offset, offset + size), res.length)
