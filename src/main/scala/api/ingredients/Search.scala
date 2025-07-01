package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.IngredientsRepo
import domain.{DbError, IngredientError, IngredientId, StorageId}

import io.circe.generic.auto.*
import com.augustnagro.magnum.magzio.Transactor
import sttp.model.StatusCode
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

private val search =
  ingredientsEndpoint
    .get
    .in("ingredients-for-storage")
    .in(query[String]("query"))
    .in(query[StorageId]("storage"))
    .out(jsonBody[Vector[IngredientSearchResult]])
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(searchHandler)

private def searchHandler(query: String, storageId: StorageId):
  ZIO[Transactor, DbError.UnexpectedDbError, Vector[IngredientSearchResult]] =
  ???