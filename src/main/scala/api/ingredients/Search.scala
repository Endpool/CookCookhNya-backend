package api.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.{IngredientsRepo, StorageIngredientsRepo}
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.{query, *}
import zio.ZIO

private type SearchAllEnv = IngredientsRepo & StorageIngredientsRepo

private val search: ZServerEndpoint[SearchAllEnv, Any] =
  ingredientsEndpoint
    .get
    .in(query[String]("query"))
    .in(query[Int]("size").default(2))
    .in(query[Int]("offset").default(0))
    .in(query[Int]("threshold").default(50))
    .out(jsonBody[SearchResp[IngredientResp]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchHandler)

private def searchHandler(
  query: String,
  size: Int,
  offset: Int,
  threshold: Int
): ZIO[AuthenticatedUser & SearchAllEnv, InternalServerError, SearchResp[IngredientResp]] =
  for
    allDbIngredients <- ZIO.serviceWithZIO[IngredientsRepo](_.getAllPersonal.orElseFail(InternalServerError()))
    allIngredients = allDbIngredients.map(IngredientResp.fromDb)
    res = Vector.from(allIngredients)
      .map(i => (i, FuzzySearch.tokenSetPartialRatio(query, i.name)))
      .filter((_, ratio) => ratio >= threshold)
      .sortBy(
        (i, ratio) => (
          -ratio, // negate the ratio to make order descending
          (i.name.length - query.length).abs // secondary sort by length difference
        )
      )
      .map(_._1)
  yield SearchResp(res.slice(offset, offset + size), res.length)
