package api

import domain.{DbError, IngredientError, StorageError, UserError}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*

object EndpointErrorVariants:
  val ingredientNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[IngredientError.NotFound]))

  val storageNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[StorageError.NotFound]))

  val userNotFoundVariant =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[UserError.NotFound]))

  val serverUnexpectedErrorVariant =
    oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[DbError.UnexpectedDbError]))

  val databaseFailureErrorVariant =
    oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[DbError.DbNotRespondingError]))
