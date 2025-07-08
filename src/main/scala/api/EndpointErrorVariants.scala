package api

import domain.{
  ErrorResponse,
  IngredientNotFound,
  StorageNotFound,
  StorageAccessForbidden,
  UserNotFound,
  RecipeNotFound,
  InternalServerError,
}

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import scala.reflect.ClassTag
import sttp.model.StatusCode.*
import sttp.tapir.EndpointOutput

extension (sc: StatusCode)
  def variantJson[E <: ErrorResponse : ClassTag]: EndpointOutput.OneOfVariant[E] =
    oneOfVariant(statusCode(sc).and(jsonBody[E]))

object EndpointErrorVariants:
  val ingredientNotFoundVariant = NotFound.variantJson[IngredientNotFound]
  val storageNotFoundVariant = NotFound.variantJson[StorageNotFound]
  val storageAccessForbiddenVariant = Forbidden.variantJson[StorageAccessForbidden]
  val userNotFoundVariant = NotFound.variantJson[UserNotFound]
  val recipeNotFoundVariant = NotFound.variantJson[RecipeNotFound]
  val serverErrorVariant = StatusCode.InternalServerError.variantJson[InternalServerError]
